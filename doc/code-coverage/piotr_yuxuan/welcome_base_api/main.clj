✔ (ns piotr-yuxuan.welcome-base-api.main
?   (:require [piotr-yuxuan.closeable-map :as closeable-map :refer [closeable-map* closeable* close-with with-tag]]
?             [piotr-yuxuan.welcome-base-api.config :as config]
?             [com.brunobonacci.oneconfig :refer [deep-merge]]
?             [malli.core :as m]
?             [malli.util :as mu]
?             [muuntaja.core :as muuntaja]
?             [org.httpkit.server :as http]
?             [reitit.coercion.malli :as rcm]
?             [reitit.ring :as ring]
?             [reitit.ring.coercion :as rrc]
?             [reitit.ring.middleware.exception :as exception]
?             [reitit.ring.middleware.muuntaja :as muuntaja-middleware]
?             [reitit.ring.middleware.parameters :as parameters]
?             [reitit.swagger :as swagger]
?             [reitit.swagger-ui :as swagger-ui]
?             [ring.util.http-response :as http-response]
?             [ring.util.http-status :as http-status])
?   (:import (piotr_yuxuan.closeable_map CloseableMap))
?   (:gen-class))
  
✔ (def Entity
✔   (m/schema
✔     [:map {:description "Description of Entity."
✔            :example {:id 1, :author "youp", :name "uo"}}
✔      [:id pos-int?]
✔      [:name string?]
✔      [:author string?]]))
  
✔ (def EntityEditScript
?   ;; https://github.com/juji-io/editscript
✔   (m/schema [any? {:description "Description of EntityEditScript."}]))
  
✔ (def EntityState
✔   (m/schema
✔     [:map {:description "Description of EntityState."}
✔      [:name string?]
✔      [:out [:sequential string?]]
✔      [:in [:sequential string?]]]))
  
✔ (def EntityQuery
✔   (let [filter-direction [:enum {:swagger/type "string"
?                                  :description "Description of a filter direction."}
?                           :blacklist :whitelist]]
✔     [:map {:description "Description of EntityQuery."}
✔      [:name [:map {:swagger {:type "object"}}
✔              [:direction filter-direction]
✔              [:values [:sequential string?]]]]
✔      [:author [:map {:swagger {:type "object"}}
✔                [:direction filter-direction]
✔                [:values [:sequential string?]]]]]))
  
✔ (def ErrorResponse
✔   [:map
✔    [:message string?]
✔    [:description string?]
✔    [:data {:optional true} map?]])
  
✔ (def ClientErrorResponse
✔   (mu/update-properties ErrorResponse assoc :description "Error caused by a request. This API is working properly."))
  
✔ (def ServerErrorResponse
✔   (mu/update-properties ErrorResponse assoc :description "Error caused internally by the server. Your request was legit, but the API failed to handle it."))
  
✔ (def DependentServiceErrorResponse
✔   (mu/update-properties ErrorResponse assoc :description "Error caused by a third-party service used by the API. Your request was legit, the API is working properly but relies on another service which failed to reply gracefully."))
  
✔ (def non-error-responses
✔   (reduce (fn [acc status] (let [{:keys [name description]} (get http-status/status status)] (assoc acc status {:description (format "_%s_. %s" name description)}))) {} [http-status/accepted http-status/created http-status/no-content http-status/not-modified http-status/ok http-status/processing http-status/reset-content]))
  
✔ (def client-errors
✔   {http-status/bad-request {:description "_Bad Request_. Syntactic error in request body, can't be parsed."}
✔    http-status/unauthorized {:description "_Unauthorized_. Issue with authentication."}
✔    http-status/payment-required {:description "_Payment Required_. The authentication is right, the authorisation would be right if the client had the correct membership or subscription."}
✔    http-status/forbidden {:description "_Forbidden_. Issue with authorisation. Authentication was right, but the client lacks permissions."}
✔    http-status/not-found {:description "_Not Found_. We can't find what you're talking about."}
✔    http-status/method-not-allowed {:description "_Method Not Allowed_. This endpoint has no handler defined for this method."}
✔    http-status/not-acceptable {:description "_Not Acceptable_. There is an issue with content negotiation, for example the client is requesting json while the server can only reply with edn."}
✔    http-status/unprocessable-entity {:description "_Unprocessable Entity_. The request can be parsed, but contains semantic errors, for example you are searching for a value lesser than 1 and at the same time greater than 2."}
✔    http-status/request-timeout {:description "_Request Timeout_. The server was waiting in a state for some time, but received no requests. It has now timed out, and is no longer is the state to accept this request."}
✔    http-status/conflict {:description "_Conflict_. The client tried to apply a diff to modify a resource, but the resource has changed since and now is different from what the client expected."}
✔    http-status/gone {:description "_Gone_. The client tries to modify something that it can no longer access to."}
✔    http-status/locked {:description "_Locked_. Some other client initiated a transaction when the request was processed, so the requested resource could not be accessed."}
✔    http-status/too-many-requests {:description "_Too Many Requests_. You are being rate-limited."
✔                                   :headers {"Retry-After" {:type "integer", :description "A non-negative decimal integer indicating the seconds to delay after the response is received."}}}
✔    http-status/unavailable-for-legal-reasons {:description "_Unavailable For Legal Reasons_. Censorship, copyright issues, or something else like GDPR prevent the server to provide access to the resource."}})
  
✔ (def server-errors
✔   {http-status/internal-server-error {:description "_Internal Server Error_. Relates to an error on the instance processing the request."}
✔    http-status/not-implemented {:description "_Not Implemented_. The code is still being worked on."}
✔    http-status/service-unavailable {:description "_Service Unavailable_. You are not sending too many requests yourself, but the server is receiving more than it can handle."}})
  
✔ (def dependent-service-errors
✔   {http-status/bad-gateway {:description "_Bad Gateway_. Processing of this request depends on another service which is up and running but returned an error."}
✔    http-status/gateway-timeout {:description "_Gateway Timeout_. Processing of this request depends on another service but it was unreachable within a timeout."}})
  
✔ (def rest-http-verbs
?   "A method is safe when it has no side effects and is read-only. A
?   method is idempotent when applying a request multiple times has the
?   same effect and yields the same result as applying it only
?   once (that is, no _side_ effects). Having the same result does not
?   necessarily mean responding with the same response. A safe method is
?   defined as idempotent. Not all responses are cacheables, it depends
?   on the status code, specific headers, and the request method.
  
?   References:
?   - `reitit.ring/http-methods`
?   - https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol
?   - https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
?   - https://developer.mozilla.org/en-US/docs/Glossary/Safe/HTTP
?   - https://developer.mozilla.org/en-US/docs/Glossary/Idempotent
?   - https://developer.mozilla.org/en-US/docs/Glossary/cacheable"
✔   {:get {:description "Requests a representation of the specified resource. Requests using GET should only be used to request data and shouldn't include data."
?          :safe? true
?          :idempotent? true
?          :cacheable? true
?          :request/body? false
?          :response.successful/body? true}
✔    :head {:description "Return only the response headers of a GET request. Can't have a body."
?           :safe? true
?           :idempotent? true
?           :cacheable? true
?           :request/body? false
?           :response.successful/body? false}
✔    :post {:description "Send data to the server, data type is indicated by the `Content-Type` header. A POST is typically sent by a html form."
?           :safe? false
?           :idempotent? false
✔           :cacheable? [nil "Only if freshness information is included."]
?           :request/body? true
?           :response.successful/body? true}
✔    :put {:description "Create a new resource or replaces a representation of the target resource with the request payload."
?          :safe? false
?          :idempotent? true
?          :cacheable? false
?          :request/body? true
?          :response.successful/body? false}
✔    :delete {:description "Delete the specified resource."
?             :safe? false
?             :idempotent? true
?             :cacheable? false
✔             :request/body? [nil "As you wish."]
✔             :response.successful/body? [nil "As you wish."]}
✔    :connect {:description "Starts two-way communications with the requested resource. It can be used to open a tunnel."
?              :safe? false
?              :idempotent? false
?              :cacheable? false
?              :request/body? false
?              :response.successful/body? true}
✔    :options {:description "Requests permitted communication options for a given URL or server. Response contains headers that describe the allowed options (http verb, or encoding)."
?              :safe? true
?              :idempotent? true
?              :cacheable? false
?              :request/body? false
?              :response.successful/body? true}
✔    :trace {:description "Request a remote, application-level loop-back of the request message."
?            :safe? true
?            :idempotent? true
?            :cacheable? false
?            :request/body? false
?            :response.successful/body? false}
✔    :patch {:description "A set of instructions on how to modify a resource that applies partial modifications to a resource."
?            :safe? false
?            :idempotent? false
?            :cacheable? false
?            :request/body? true
?            :response.successful/body? true}})
  
✔ (def ExchangeId [pos-int? {:swagger {:example "4634"}}])
✔ (def SecurityId [pos-int? {:swagger {:example "6237483"}}])
✔ (def OrderUuid [uuid? {:swagger {:example "ea9b2871-7f49-4982-824a-9ab3db2b385a"}}])
  
✔ (def routes
✔   [["/api/v1" {:swagger {:security [{"X-API-Key" []
✔                                      "Authorization" []}]}}
✔     ["/shared-requests-responses" {:tags #{"Shared requests and responses"}
?                                    :summary "Documentation only"
?                                    :description "Common HTTP responses are reported here so as to document only endpoint-specific responses hereafter. This is not restricted to `GET` method but there would be no point in duplicating this list for other methods. Depending on the endpoint you target, some attributes may not always apply."
✔                                    :get {:handler (constantly (http-response/not-implemented))
✔                                          :parameters {:header [:map
✔                                                                ["X-API-Key" {:optional true} [string? {:description "Authentication"}]]
✔                                                                ["Authorization" {:optional true} [string? {:description "Authorization"}]]]
✔                                                       :params {}}
✔                                          :responses (-> (merge (update-vals client-errors #(assoc % :schema ClientErrorResponse))
✔                                                                (update-vals server-errors #(assoc % :schema ServerErrorResponse))
✔                                                                (update-vals dependent-service-errors #(assoc % :schema DependentServiceErrorResponse)))
✔                                                         (update-vals #(update % :headers merge {"X-Error-UUID" {:type "string", :description "UUID for this error, linked to technical details in our observability stack."}}))
✔                                                         (merge non-error-responses)
✔                                                         (update-vals #(update % :headers merge {"Date" {:type "date", :description "The date and time at which the message was originated, in HTTP date format."}
✔                                                                                                 "X-B3-TraceId" {:type "string", :description "Zipkin-specific header, 128 or 64 lower-hex encoded bits (required)"}
✔                                                                                                 "X-B3-SpanId" {:type "string", :description "Zipkin-specific header, 64 lower-hex encoded bits (required)"}
✔                                                                                                 "X-B3-ParentSpanId" {:type "string", :description "Zipkin-specific header, 64 lower-hex encoded bits (absent on root span)"}
✔                                                                                                 "X-B3-Sampled" {:type "string", :description "Zipkin-specific header, Boolean (either “1” or “0”, can be absent)"}
✔                                                                                                 "X-B3-Flags" {:type "string", :description "Zipkin-specific header, “1” means debug (can be absent)"}})))}}]
✔     ["/exchanges"
✔      ["" {:tags #{"Exchanges"}
✔           :get {:summary "List existing exchanges"
✔                 :handler (constantly (http-response/ok))}
✔           :post {:summary "Create an exchange"
✔                  :handler (constantly (http-response/ok))}}]
✔      ["/:exchange-id"
✔       ["" {:tags #{"Exchanges"}
✔            :post {:summary "Update this exchange"
✔                   :handler (constantly (http-response/ok))
✔                   :parameters {:path [:map [:exchange-id ExchangeId]]}}
✔            :get {:summary "Describe this exchange"
✔                  :handler (constantly (http-response/ok))
✔                  :parameters {:path [:map [:exchange-id ExchangeId]]}}
✔            :patch {:summary "Apply some transformations on this exchange"
✔                    :handler (constantly (http-response/ok))
✔                    :parameters {:path [:map [:exchange-id ExchangeId]]}}
✔            :delete {:summary "Archive this exchange"
✔                     :handler (constantly (http-response/ok))
✔                     :parameters {:path [:map [:exchange-id ExchangeId]]}}}]
✔       ["/listing" {:tags #{"Securities"}}
✔        ["" {:get {:summary "List securities registered on this exchange"
✔                   :handler (constantly (http-response/ok))
✔                   :parameters {:path [:map [:exchange-id ExchangeId]]}}
✔             :post {:summary "Register a securities on the listing of this exchange"
✔                    :handler (constantly (http-response/ok))
✔                    :parameters {:path [:map [:exchange-id ExchangeId]]}}}]
✔        ["/:security-id"
✔         ["" {:post {:summary "Update the details of this listing"
✔                     :handler (constantly (http-response/ok))
✔                     :parameters {:path [:map
✔                                         [:exchange-id ExchangeId]
✔                                         [:security-id SecurityId]]}}
✔              :get {:summary "Describe the details of this listing"
✔                    :handler (constantly (http-response/ok))
✔                    :parameters {:path [:map
✔                                        [:exchange-id ExchangeId]
✔                                        [:security-id SecurityId]]}}
✔              :patch {:summary "Apply some transformations on the details of this listing"
✔                      :handler (constantly (http-response/ok))
✔                      :parameters {:path [:map
✔                                          [:exchange-id ExchangeId]
✔                                          [:security-id SecurityId]]}}
✔              :delete {:summary "Archive this security"
✔                       :handler (constantly (http-response/ok))
✔                       :parameters {:path [:map
✔                                           [:exchange-id ExchangeId]
✔                                           [:security-id SecurityId]]}}}]
?         ;; While REST recommends that URL are only made of nouns, here are some verbs to update the state of a listing.
✔         ["/enlist" {:post {:summary "Make this listing available for trade on this exchange"
✔                            :handler (constantly (http-response/ok))
✔                            :parameters {:path [:map
✔                                                [:exchange-id ExchangeId]
✔                                                [:security-id SecurityId]]}}}]
✔         ["/delist" {:post {:summary "Make this listing unavailable for trade on this exchange"
✔                            :handler (constantly (http-response/ok))
✔                            :parameters {:path [:map
✔                                                [:exchange-id ExchangeId]
✔                                                [:security-id SecurityId]]}}}]]]
  
✔       ["/book"
✔        ["/orders"
✔         [["" {:tags #{"Orders"}
✔               :post {:summary "Place an order on the book. Its status may vary, but its content is immutable."
✔                      :handler (fn [request]
✘                                 (println :request request)
✘                                 (http-response/ok))
✔                      :parameters {:path [:map [:exchange-id ExchangeId]]
✔                                   :body [:map
✔                                          [:exchange-id ExchangeId]
✔                                          [:order-uuid OrderUuid]]}
✔                      :responses {http-status/accepted {:description (let [{:keys [name description]} (get http-status/status http-status/accepted)]
✔                                                                       (format "_%s_. %s" name description))
✔                                                        :headers {"Content-Location" {:description "Relative URI to order details"
?                                                                                      :type "string"}
✔                                                                  "X-Order-UUID" {:description "Identifier of the order"
?                                                                                  :type "uuid"}}
✔                                                        :body [:map
✔                                                               [:exchange-id ExchangeId]
✔                                                               [:order-uuid OrderUuid]]}}}}]
✔          ["/:order-uuid"
✔           [["" {:tags #{"Orders"}
✔                 :get {:summary "Return the order as it was placed"
✔                       :handler (constantly (assoc-in (http-response/ok) [:headers "Cache-Control"] "immutable"))
✔                       :parameters {:header [:map ["Want-Digest" {:optional true} [:enum {:swagger/type "string"
?                                                                                          :description "Ask the server to provide a digest of the requested resource using the Digest response header."}
?                                                                                   "unixsum" "unixcksum" "crc32c" "sha-256" "sha-512" "id-sha-256" "id-sha-512"]]]
✔                                    :path [:map
✔                                           [:exchange-id ExchangeId]
✔                                           [:order-uuid OrderUuid]]}
✔                       :responses {http-status/ok {:headers {"Cache-Control" {:type "string"
?                                                                              :description "Control caching. As orders are immutable, they may be cached."
?                                                                              :example "immutable"}
✔                                                             "Digest" {:type "string"
?                                                                       :required false
?                                                                       :description "Digest of the selected representation of the requested resource."
?                                                                       :example "sha-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE="}}}}}}]
✔            ["/cancel" {:tags #{"Orders"}
✔                        :post {:summary "Tentatively cancel this order."
?                               :description "Tentatively cancel this order. This will be recorded, but may fail if the order had already been fulfilled."
✔                               :handler (constantly (http-response/ok))
✔                               :parameters {:path [:map
✔                                                   [:exchange-id ExchangeId]
✔                                                   [:order-uuid OrderUuid]]}}}]
✔            ["/status" {:tags #{"Orders"}
✔                        :post {:summary "History and outcome"
✔                               :handler (constantly (http-response/ok))
✔                               :parameters {:path [:map
✔                                                   [:exchange-id ExchangeId]
✔                                                   [:order-uuid OrderUuid]]}}}]]]]]]]]]])
  
✔ (def swagger-api-description
?   "
? TODO Fix me
  
? ## Description
  
? Progressive human description with incremental semantic zoom.
  
? ## Sequence diagrams
  
? ## Order matcher algorithm
  
? Example state machine
  
? ![](https://raw.githubusercontent.com/BrunoBonacci/optimus/master/docs/optimus/wsd/00-tx-status-02.png)
  
? ## Endpoint description
  
? Only most specific request and response attributes are described for each endpoint. A general prototype for request and responses may be found under the pseudo endpoint [`/api/v1/_shared-responses`](/api-docs/index.html#/Shared%20requests%20and%20responses).
? ")
  
✔ (defn swagger-doc
?   [{:keys [version]}]
✘   {:swagger "2.0"
✘    :info {:title "welcome-data-api"
✘           :version version
✘           :description swagger-api-description
?           :basePath "/"
✘           :licence {:name "European Union Public License 1.2 or later"
?                     :url "https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12"}
✘           :contact {:name "胡雨軒 Петр"
?                     :url "https://github.com/piotr-yuxuan"}}
✘    :definitions {"EntityQuery" (malli.swagger/transform EntityQuery)
✘                  "Entity" (malli.swagger/transform Entity)
✘                  "EntityEditScript" (malli.swagger/transform EntityEditScript)
✘                  "EntityState" (malli.swagger/transform EntityState)
✘                  "ClientErrorResponse" (malli.swagger/transform ClientErrorResponse)
✘                  "ServerErrorResponse" (malli.swagger/transform ServerErrorResponse)
✘                  "DependentServiceErrorResponse" (malli.swagger/transform DependentServiceErrorResponse)}
✘    :responses (merge (update-vals client-errors #(assoc % :content {:application/json {:schema {"$ref" "#/definitions/ClientErrorResponse"}}}))
✘                      (update-vals server-errors #(assoc % :content {:application/json {:schema {"$ref" "#/definitions/ServerErrorResponse"}}}))
✘                      (update-vals dependent-service-errors #(assoc % :content {:application/json {:schema {"$ref" "#/definitions/DependentServiceErrorResponse"}}})))
✘    :securityDefinitions {"X-API-Key" {:type :apiKey
?                                       :in :header
?                                       :name "X-API-Key"}
?                          ;; Bearer auth is only oas 3.0, not swagger 2.0
✘                          "Authorization" {:type :apiKey
?                                           :in :header
?                                           :description "OpenAPI 2.0 doesn't have specific support of bearer auth like OpenAPI 3.0, but it is quite similar to API key auth. The header format is `Authorization: Bearer <token>`. Here a simple JWT token is used."
?                                           :name "Authorization"}}
✘    :externalDocs {:description "cljdoc"
?                   :url "https://cljdoc.org/d/piotr-yuxuan/welcome-data-api"}})
  
✔ (defn ->router
?   [config ref!]
✘   (ring/router
✘     [routes
✘      ["" {:tags #{"Observability"}}
✘       ["/health" {:get {:handler (constantly (http-response/no-content))
✘                         :responses {http-status/no-content {:description "No Content"}}}}]
✘       ["/prometheus" {:get {:handler (constantly (http-response/not-implemented))}}]]
✘      ["" {:no-doc true}
✘       ["/swagger.json" {:get {:swagger (swagger-doc config)
✘                               :handler (swagger/create-swagger-handler)
✘                               :middleware [;swagger-2->openapi-3/middleware
?                                            ]}}]
✘       ["/api-docs/*" {:get (swagger-ui/create-swagger-ui-handler {:url "/swagger.json"})}]]]
✘     {:data {:muuntaja (muuntaja/create (assoc muuntaja/default-options :return :bytes))
✘             :coercion (rcm/create {:error-keys #{:value :humanized}})
✘             :middleware [swagger/swagger-feature ; swagger feature
✘                          parameters/parameters-middleware ;; query-params & orm-params
✘                          muuntaja-middleware/format-middleware ;; content-egotiation, request, and response
✘                          exception/exception-middleware ; exception handling
✘                          rrc/coerce-exceptions-middleware ; What is the difference?
✘                          rrc/coerce-response-middleware ; coercing response bodys
✘                          rrc/coerce-request-middleware ; coercing request parameters
?                          ]}}))
  
✔ (defn ->handler
?   [config]
✘   (let [router (atom nil)]
✘     (reset! router (->router config router))
✘     (ring/ring-handler
✘       @router
✘       (ring/routes
✘         (ring/redirect-trailing-slash-handler)
✘         (ring/create-default-handler))
?        ;; Injecting them is necessary for Swagger, but slows quite down the request processing. Inject it dynamically.
✘       {:inject-match? true
?        :inject-router? true})))
  
✔ (defn ^CloseableMap config->app
?   [->handler {:keys [port] :as config}]
✘   (closeable-map*
✘     {:http-server (with-tag ::closeable-map/fn
✘                     (closeable* (http/run-server
✘                                   (->handler config)
✘                                   {:port port})))}))
  
~ (defonce app
✘   (atom nil))
  
✔ (defn -main
?   [& args]
✘   (->> (config/load-config)
✘        (config->app ->handler)
✘        (reset! app)))
