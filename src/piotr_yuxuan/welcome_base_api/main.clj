(ns piotr-yuxuan.welcome-base-api.main
  (:require [piotr-yuxuan.closeable-map :refer [closeable-map* closeable*]]
            [com.brunobonacci.oneconfig :refer [deep-merge]]
            [ring.util.http-status :as http-status]
            [aleph.http :as http]
            [reitit.ring.middleware.parameters :as parameters]
            [piotr-yuxuan.welcome-base-api.config :as config]
            [piotr-yuxuan.welcome-base-api.swagger2 :as swagger2]
            [reitit.ring.middleware.exception :as exception]
            [ring.middleware.reload :as reload]
            [reitit.ring.coercion :as rrc]
    ;[reitit.coercion.malli :as rcm]
            [piotr-yuxuan.welcome-base-api.malli2 :as rcm]
            [reitit.ring.middleware.muuntaja :as muuntaja-middleware]
            [muuntaja.core :as muuntaja]
            [ring.util.http-response :as http-response]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [malli.util :as mu]
            [malli.core :as m])
  (:gen-class)
  (:import (piotr_yuxuan.closeable_map CloseableMap)))

(def Entity
  (m/schema
    [:map {:description "Description of Entity."
           :example {:id 1, :author "youp", :name "uo"}}
     [:id pos-int?]
     [:name string?]
     [:author string?]]))

(def EntityEditScript
  ;; https://github.com/juji-io/editscript
  (m/schema [any? {:description "Description of EntityEditScript."}]))

(def EntityState
  (m/schema
    [:map {:description "Description of EntityState."}
     [:name string?]
     [:out [:sequential string?]]
     [:in [:sequential string?]]]))

(def EntityQuery
  (let [filter-direction [:enum {:swagger/type "string"
                                 :description "Description of a filter direction."}
                          :blacklist :whitelist]]
    [:map {:description "Description of EntityQuery."}
     [:name [:map {:swagger {:type "object"}}
             [:direction filter-direction]
             [:values [:sequential string?]]]]
     [:author [:map {:swagger {:type "object"}}
               [:direction filter-direction]
               [:values [:sequential string?]]]]]))

(def ErrorResponse
  [:map
   [:message string?]
   [:description string?]
   [:error-id string?]
   [:data {:optional? true} map?]])

(def ClientErrorResponse
  (mu/update-properties ErrorResponse assoc :description "Error caused by a request. This API is working properly."))

(def ServerErrorResponse
  (mu/update-properties ErrorResponse assoc :description "Error caused internally by the server. Your request was legit, but the API failed to handle it."))

(def DependentServiceErrorResponse
  (mu/update-properties ErrorResponse assoc :description "Error caused by a third-party service used by the API. Your request was legit, the API is working properly but relies on another service which failed to reply gracefully."))

(def non-success-http-responses
  (let [client-errors {http-status/bad-request {:description "_Bad Request_: syntactic error in request body, can't be parsed."}
                       http-status/unauthorized {:description "_Unauthorized_: issue with authentication."}
                       http-status/payment-required {:description "_Payment Required_: the authentication is right, the authorisation would be right if the client had the correct membership or subscription."}
                       http-status/forbidden {:description "_Forbidden_: issue with authorisation. Authentication was right, but the client lacks permissions."}
                       http-status/not-found {:description "_Not Found_: we can't find what you're talking about."}
                       http-status/method-not-allowed {:description "_Method Not Allowed_: this endpoint has no handler defined for this method."}
                       http-status/not-acceptable {:description "_Not Acceptable_: there is an issue with content negotiation, for example the client is requesting json while the server can only reply with edn."}
                       http-status/unprocessable-entity {:description "_Unprocessable Entity_: the request can be parsed, but contains semantic errors, for example you are searching for a value lesser than 1 and at the same time greater than 2."}
                       http-status/request-timeout {:description "_Request Timeout_: the server was waiting in a state for some time, but received no requests. It has now timed out, and is no longer is the state to accept this request."}
                       http-status/conflict {:description "_Conflict_: the client tried to apply a diff to modify a resource, but the resource has changed since and now is different from what the client expected."}
                       http-status/gone {:description "_Gone_: the client tries to modify something that got previously deleted."}
                       http-status/locked {:description "_Locked_: some other client initiated a transaction when the request was processed, so the requested resource could not be accessed."}
                       http-status/too-many-requests {:description "_Too Many Requests_: you are being rate-limited."}
                       http-status/unavailable-for-legal-reasons {:description "_Unavailable For Legal Reasons_: censorship, copyright issues, or something else like GDPR prevent the server to provide access to the resource."}}
        server-errors {http-status/internal-server-error {:description "_Internal Server Error_: relates to an error on the instance processing the request."}
                       http-status/not-implemented {:description "_Not Implemented_: the code is still being worked on."}
                       http-status/service-unavailable {:description "_Service Unavailable_: you are not sending too many requests yourself, but the server is receiving more than it can handle."}}
        dependent-service-errors {http-status/bad-gateway {:description "_Bad Gateway_: processing of this request depends on another service which is up and running but returned an error."}
                                  http-status/gateway-timeout {:description "_Gateway Timeout_: processing of this request depends on another service but it was unreachable within a timeout."}}]
    (merge (update-vals client-errors #(assoc % :content {:application/json {:schema {"$ref" "#/components/schemas/ClientErrorResponse"}}}))
           (update-vals server-errors #(assoc % :content {:application/json {:schema {"$ref" "#/components/schemas/ServerErrorResponse"}}}))
           (update-vals dependent-service-errors #(assoc % :content {:application/json {:schema {"$ref" "#/components/schemas/DependentServiceErrorResponse"}}})))))

(def rest-http-verbs
  "A method is safe when it has no side effects and is read-only. A
  method is idempotent when applying a request multiple times has the
  same effect and yields the same result as applying it only
  once (that is, no _side_ effects). Having the same result does not
  necessarily mean responding with the same response. A safe method is
  defined as idempotent. Not all responses are cacheables, it depends
  on the status code, specific headers, and the request method.

  References:
  - `reitit.ring/http-methods`
  - https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol
  - https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
  - https://developer.mozilla.org/en-US/docs/Glossary/Safe/HTTP
  - https://developer.mozilla.org/en-US/docs/Glossary/Idempotent
  - https://developer.mozilla.org/en-US/docs/Glossary/cacheable"
  {:get {:description "Requests a representation of the specified resource. Requests using GET should only be used to request data and shouldn't include data."
         :safe? true
         :idempotent? true
         :cacheable? true
         :request/body? false
         :response.successful/body? true}
   :head {:description "Return only the response headers of a GET request. Can't have a body."
          :safe? true
          :idempotent? true
          :cacheable? true
          :request/body? false
          :response.successful/body? false}
   :post {:description "Send data to the server, data type is indicated by the `Content-Type` header. A POST is typically sent by a html form."
          :safe? false
          :idempotent? false
          :cacheable? [nil "Only if freshness information is included."]
          :request/body? true
          :response.successful/body? true}
   :put {:description "Create a new resource or replaces a representation of the target resource with the request payload."
         :safe? false
         :idempotent? true
         :cacheable? false
         :request/body? true
         :response.successful/body? false}
   :delete {:description "Delete the specified resource."
            :safe? false
            :idempotent? true
            :cacheable? false
            :request/body? [nil "As you wish."]
            :response.successful/body? [nil "As you wish."]}
   :connect {:description "Starts two-way communications with the requested resource. It can be used to open a tunnel."
             :safe? false
             :idempotent? false
             :cacheable? false
             :request/body? false
             :response.successful/body? true}
   :options {:description "Requests permitted communication options for a given URL or server. Response contains headers that describe the allowed options (http verb, or encoding)."
             :safe? true
             :idempotent? true
             :cacheable? false
             :request/body? false
             :response.successful/body? true}
   :trace {:description "Request a remote, application-level loop-back of the request message."
           :safe? true
           :idempotent? true
           :cacheable? false
           :request/body? false
           :response.successful/body? false}
   :patch {:description "A set of instructions on how to modify a resource that applies partial modifications to a resource."
           :safe? false
           :idempotent? false
           :cacheable? false
           :request/body? true
           :response.successful/body? true}})

(slurp (muuntaja/encode "application/json" {:id 1, :author "vil!", :name "uo"}))
(slurp (muuntaja/encode "application/transit+json" {:id 1, :author "vil!", :name "uo"}))

(defn routes
  [config]
  [["/health"]
   ["/api/v1"
    ["/entities"
     ;; It is very difficult to choose between these two.
     ;; The commented looks like the purest, but it's insane
     ;; to waste a network hop on creation.
     #_["" {:put {:handler (constantly (http-response/ok :create))
                  :parameters {:body (mu/dissoc Entity :id)}
                  :responses {:header [:map [:location (mu/get Entity [:id])]]
                              :body nil?}}
            :post {:handler (constantly (http-response/ok :query))
                   :parameters {:body EntityQuery}
                   :responses {http-status/ok {:body [:sequential Entity]}}}}]
     ["" {:post {:summary "Create a new entity"
                 :description "Longer description, create an entity."
                 :handler (constantly (http-response/ok :create))
                 :parameters {:body (mu/dissoc Entity :id)
                              :examples {"application/json" {:id 1, :author "parameters-examples", :name "uo"}}}
                 :responses {http-status/created {:body Entity
                                                  :examples {"application/json" {:id 1, :author "response-created-examples", :name "uo"}}
                                                  :headers {"x-api-key" {:description "Your key"
                                                                         :type "integer"}}}}}
          :put {:summary "Define a new entity entirely"
                :handler (constantly (http-response/ok :defined-entirely))
                :parameters {:body Entity}
                :responses {http-status/created {:description "_No Content_"}
                            :errors {:description "Youp"
                                     :body ClientErrorResponse}}}
          :get {:summary "Query entities"
                :description "Longer description, query entities with query params."
                :handler (constantly (http-response/ok :query))
                :parameters {:query EntityQuery}
                ;:responses {http-status/ok {:body Entity}}
                :responses {200 {:body [:map [:total int?]]}}}}]
     ["/:id" {:get {:handler (constantly (http-response/ok :get))
                    :parameters {:path (mu/select-keys Entity [:id])}
                    :responses {http-status/ok {:body Entity}}}
              ;; https://softwareengineering.stackexchange.com/questions/114156/why-are-there-no-put-and-delete-methods-on-html-forms
              ;; For pure html forms on non-scripted browsers, only POST and GET are available and no other methods. Use the _method trick to talk to a RESTful API, or is it a case for CQRS with a command endpoint? When JavaScript is used, the problem is solved by using a XmlHttpRequest with the correct method.
              :put {:handler (constantly (http-response/ok :update))
                    :parameters {:path (mu/select-keys Entity [:id])
                                 :body Entity}
                    :responses {http-status/no-content {:body nil?}
                                http-status/not-modified {:body nil?}}}
              :patch {:handler (constantly (http-response/ok :update))
                      :parameters {:path (mu/select-keys Entity [:id])
                                   :body EntityEditScript}
                      :responses {http-status/ok {:body Entity}
                                  (get http-status/status http-status/not-modified) {:body nil?}}}
              :delete {:handler (constantly (http-response/ok :delete))
                       :parameters {:path (mu/select-keys Entity [:id])}
                       :responses {http-status/reset-content {:body nil?}}}}]
     ;; Examples of how to update a state machine:
     ["/:id/state" {:get {:handler (constantly (http-response/ok :state))
                          :parameters {:path (mu/select-keys Entity [:id])}
                          :responses {http-status/ok {:body EntityState}}}
                    :post {:summary "asd"
                           :description "# asdasd"
                           :handler (constantly (http-response/ok :state-transition))
                           :parameters {:path (mu/select-keys Entity [:id])
                                        :body [:map [:transition [:enum :start :stop]]]}
                           :responses {http-status/ok {:body EntityState}
                                       http-status/accepted {:body EntityState}
                                       http-status/processing {:body EntityState}}}}]]]])

(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defonce app (atom nil))
(println "reloaded")

(defn swagger-doc
  [{:keys [version]}]
  {:swagger "2.0"
   :info {:title "welcome-data-api"
          :version version
          :description "For each endpoint this documentation lists the most specific responses. Some server-wide responses for success and failures "
          :basePath "/"
          :licence {:name "European Union Public License 1.2 or later"
                    :url "https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12"}
          :contact {:name "胡雨軒 Петр"
                    :url "https://github.com/piotr-yuxuan"}}
   :definitions {"EntityQuery" (malli.swagger/transform EntityQuery)
                 "Entity" (malli.swagger/transform Entity)
                 "EntityEditScript" (malli.swagger/transform EntityEditScript)
                 "EntityState" (malli.swagger/transform EntityState)
                 "ClientErrorResponse" (malli.swagger/transform ClientErrorResponse)
                 "ServerErrorResponse" (malli.swagger/transform ServerErrorResponse)
                 "DependentServiceErrorResponse" (malli.swagger/transform DependentServiceErrorResponse)}
   :responses non-success-http-responses
   :externalDocs {:description "cljdoc"
                  :url "https://cljdoc.org/d/piotr-yuxuan/welcome-data-api"}})

(def openapi-middleware
  (fn [handler]
    (fn [request]
      (update (handler request) :body
        #(-> % (dissoc :swagger) (assoc :openapi "3.0.0"))))))

(defonce debug (atom nil))
:examples {"application/json" "[1]"}

(defn ^CloseableMap config->app
  [{:keys [env port] :as config}]
  (closeable-map*
    (-> config
        (assoc :http-server (closeable* (http/start-server
                                          (fn dev-handler [request]
                                            (println :youp)
                                            ((ring/ring-handler
                                               (ring/router
                                                 [(routes config)
                                                  ["" {:no-doc true}
                                                   ["/swagger.json" {:get {:swagger (swagger-doc config)
                                                                           :handler (swagger2/create-swagger-handler)}}]
                                                   ["/api-docs/*" {:get (swagger-ui/create-swagger-ui-handler {:url "/swagger.json"})}]]]
                                                 {:data {:muuntaja muuntaja/instance
                                                         :coercion (rcm/create {:error-keys #{:value :humanized}})
                                                         :middleware [;; swagger feature
                                                                      swagger/swagger-feature
                                                                      ;; query-params & form-params
                                                                      parameters/parameters-middleware
                                                                      ;; content-negotiation, request, and response
                                                                      muuntaja-middleware/format-middleware
                                                                      ;; exception handling
                                                                      exception/exception-middleware
                                                                      rrc/coerce-exceptions-middleware ; What is the difference?
                                                                      ;; coercing response bodys
                                                                      rrc/coerce-response-middleware
                                                                      ;; coercing request parameters
                                                                      rrc/coerce-request-middleware

                                                                      ]}})
                                               (ring/routes
                                                 (ring/redirect-trailing-slash-handler)
                                                 (ring/create-default-handler))
                                               {:middleware [reload/wrap-reload]})
                                             request))
                                          {:port port}))))))

(defn start
  ([] (start app))
  ([app] (start app (config/load-config)))
  ([app config]
   (when-not @app
     (reset! app (config->app config)))
   app))

(defn stop
  ([] (stop app))
  ([app]
   (when @app
     (.close ^CloseableMap @app)
     (reset! app nil))
   app))

(defn reset
  ([] (reset app))
  ([app] (reset app (config/load-config)))
  ([app config]
   (stop app)
   (start app config)))

(defn -main
  [& args]
  (greet {:name (first args)})
  (start)
  (reset)
  )

(reset)
