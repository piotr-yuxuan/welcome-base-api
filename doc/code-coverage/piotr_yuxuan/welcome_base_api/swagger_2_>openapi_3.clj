✔ (ns piotr-yuxuan.welcome-base-api.swagger-2->openapi-3
?   "See https://gist.github.com/czan/8752cacf527ceb0e64982b9fa8747892"
?   (:require [camel-snake-kebab.core :refer [->PascalCase]]
?             [clojure.set :refer [rename-keys]]
?             [clojure.walk :as walk]
?             [reitit.coercion.malli :as malli]))
  
? ;; Issues: doesn't preserve endpoint order.
  
✔ (defn ^{:deprecated true} map-vals
?   "Use `clojure.core/update-vals` instead."
?   [f m]
✘   (into {} (map (fn [[k v]] [k (f v)])) m))
  
✔ (defn upgrade-to-openapi-v3
?   [specification route-data]
✘   (let [schemas (volatile! {})]
✘     (letfn [(clean-schema-title [title]
✘               (when-not (false? (::component-schemas route-data))
✘                 (condp = (:coercion route-data)
✘                   malli/coercion (->PascalCase title :separator #"[/-]")
✘                   (throw (ex-info "Unknown coercion function, can't automatically map name to OpenAPI spec."
✘                                   {:coercion (:coercion route-data)})))))
  
?             (extract-schema [object]
✘               (or (when-let [title (and (:type object) (:title object) (clean-schema-title (:title object)))]
✘                     (let [schema (assoc object :title title)]
✘                       (when-let [old-schema (get @schemas title)]
✘                         (when-not (= schema old-schema)
✘                           (throw (ex-info "Multiple schemas with the same name but different definitions"
✘                                           {:name title
✘                                            :schemas [schema old-schema]}))))
✘                       (vswap! schemas assoc title schema)
✘                       {"$ref" (str "#/components/schemas/" title)}))
✘                   (cond-> object
✘                     (:x-nullable object) (rename-keys {:x-nullable :nullable}))))
  
?             (transform-parameter [parameter]
✘               (if (:schema parameter)
✘                 parameter
✘                 (assoc (dissoc parameter :type :format :enum)
✘                   :schema (-> parameter
✘                               (select-keys [:type :format :enum :x-nullable])
✘                               (rename-keys {:x-nullable :nullable})))))
  
?             (transform-endpoint [{:keys [produces responses] :as endpoint}]
✘               (let [fixed-parameters (map transform-parameter (:parameters endpoint))
✘                     body-parameter (first (filter #(= (:in %) "body") fixed-parameters))
✘                     non-body-parameters (remove #(= (:in %) "body") fixed-parameters)
✘                     fixed-body-parameter (when body-parameter
✘                                            {:required (:required body-parameter)
✘                                             :content (into {}
✘                                                            (map (fn [content-type]
✘                                                                   [content-type (select-keys body-parameter [:schema])]))
✘                                                            produces)})
✘                     fixed-responses (map-vals (fn [response]
✘                                                 (let [content (into {}
✘                                                                     (map (fn [content-type]
✘                                                                            [content-type (select-keys response [:schema])]))
✘                                                                     produces)]
✘                                                   (assoc (dissoc response :schema)
✘                                                     :content content)))
✘                                               responses)]
✘                 (cond-> (dissoc endpoint :produces :consumes)
✘                   non-body-parameters (assoc :parameters non-body-parameters)
✘                   fixed-body-parameter (assoc :requestBody fixed-body-parameter)
✘                   fixed-responses (assoc :responses fixed-responses))))
  
?             (transform-path [path]
✘               (map-vals transform-endpoint path))]
✘       (-> (walk/postwalk extract-schema specification)
✘           (assoc-in [:components :schemas] @schemas)
✘           (update :paths #(map-vals transform-path %))
✘           (dissoc :swagger)
✘           (assoc :openapi "3.0.0")))))
  
✔ (def middleware
✔   {:name ::extract-swagger-json-definitions
✔    :compile (fn [route-data _opts]
✘               (fn [handler]
✘                 (fn [request]
✘                   (update (handler request) :body upgrade-to-openapi-v3 route-data))))})
  
? ;; Then your Swagger endpoint can look something like this:
? ;;
? ;; ["/swagger.json"
? ;;  {:get {:no-doc     true
? ;;         :swagger    {:basePath   "/"}
? ;;         :middleware [swagger->openapi]
? ;;         :handler    (swagger/create-swagger-handler)}}]
