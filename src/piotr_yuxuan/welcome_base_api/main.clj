(ns piotr-yuxuan.welcome-base-api.main
  (:require [piotr-yuxuan.closeable-map :refer [closeable-map]]
            [ring.util.http-status :as http-status]
            [reitit.core :as r]
            [reitit.ring :as ring]
            [malli.util :as mu])
  (:gen-class))

(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

(def Entity
  (m/schema
    [:map
     [:id pos-int?]
     [:name string?]
     [:author string?]]))

(def EntityQuery
  (let [filter-direction [:enum :blacklist :whitelist]]
    [:map
     [:name [:map
             [:direction filter-direction]
             [:values [:sequential string?]]]]
     [:author [:map
               [:direction filter-direction]
               [:values [:sequential string?]]]]]))

(def non-success-http-statuses
  {;; Client errors
   http-status/bad-request "Syntactic error in request body, can't be parsed."
   http-status/unauthorized "Issue with authentication."
   http-status/payment-required "The authentication is right, the authorisation would be right if the client had the correct membership or subscription."
   http-status/forbidden "Authentication was right, but the client lacks permissions."
   http-status/not-found "We can't find what you're talking about."
   http-status/method-not-allowed "This endpoint has no handler defined for this method."
   http-status/not-acceptable "There is an issue with content negotiation, for example the client is requesting json while the server can only reply with edn."
   http-status/unprocessable-entity "The request can be parsed, but contains semantic errors, for example you are searching for a value lesser than 1 and at the same time greater than 2."
   http-status/request-timeout "The server was waiting in a state for some time, but received no requests. It has now timed out, and is no longer is the state to accept this request."
   http-status/conflict "The client tried to apply a diff to modify a resource, but the resource has changed since and now is different from what the client expected."
   http-status/gone "The client tries to modify something that got previously deleted."
   http-status/locked "Some other client initiated a transaction when the request was processed, so the requested resource could not be accessed."
   http-status/too-many-requests "You are being rate-limited."
   http-status/unavailable-for-legal-reasons "Censorship, copyright issues, or something else like GDPR prevent the server to provide access to the resource."
   ;; Server errors
   http-status/internal-server-error "Relates to an error on the instance processing the request."
   http-status/not-implemented "The code is still being worked on."
   http-status/service-unavailable "You are not sending too many requests yourself, but the server is receiving more than it can handle."
   ;; Depending service errors
   http-status/bad-gateway "Processing of this request depends on another service which is up and running but returned an error."
   http-status/gateway-timeout "Processing of this request depends on another service but it was unreachable within a timeout."})

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

(defn routes
  []
  ["/api/v1"
   ["/entities"
    ;; It is very difficult to choose between these two.
    ;; The commented looks like the purest, but it's insane
    ;; to waste a network hop on creation.
    #_["" {:put {:handler (constantly :create)
                 :parameters {:body (mu/dissoc Entity [:id])}
                 :responses {:header [:map [:location (mu/get Entity [:id])]]
                             :body nil?}}
           :post {:handler (constantly :query)
                  :parameters {:body EntityQuery}
                  :responses {http-status/ok {:body [:sequential Entity]}}}}]
    ["" {:post {:handler (constantly :create)
                :parameters {:body (mu/dissoc Entity [:id])}
                :responses {http-status/created {:body Entity}}}
         :get {:handler (constantly :query)
               :parameters {:query EntityQuery}
               :responses {http-status/ok {:body [:sequential Entity]}}}}]
    ["/:id" {:get {:handler (constantly :get)
                   :parameters {:path (mu/select-keys Entity [:id])
                                ;; Here the params could help select the fields.
                                :query [:map]}
                   :responses {http-status/ok {:body Entity}}}
             ;; https://softwareengineering.stackexchange.com/questions/114156/why-are-there-no-put-and-delete-methods-on-html-forms
             ;; For pure html forms on non-scripted browsers, only POST and GET are available and no other methods. Use the _method trick to talk to a RESTful API, or is it a case for CQRS with a command endpoint? When JavaScript is used, the problem is solved by using a XmlHttpRequest with the correct method.
             :put {:handler (constantly :update)
                   :parameters {:path (mu/select-keys Entity [:id])
                                :body Entity}
                   :responses {http-status/no-content {:body nil?}
                               http-status/not-modified {:body nil?}}}
             :patch {:handler (constantly :update)
                     :parameters {:path (mu/select-keys Entity [:id])
                                  :body Entity}
                     :responses {http-status/ok {:body Entity}
                                 http-status/not-modified {:body nil?}}}
             :delete {:handler (constantly :delete)
                      :parameters {:path (mu/select-keys Entity [:id])}
                      :responses {http-status/reset-content {:body nil?}}}}]
    ;; Examples of how to update a state machine.
    ["/:id/start" {:post {:handler (constantly :start)
                          :parameters {:path (mu/select-keys Entity [:id])}
                          :responses {http-status/accepted {:body nil?}}}}]
    ["/:id/stop" {:post {:handler (constantly :stop)
                         :parameters {:path (mu/select-keys Entity [:id])}
                         :responses {http-status/accepted {:body Entity}}}}]
    ["/:id/status" {:get {:handler (constantly :status)
                          :responses {http-status/ok {:body [:map {:closed false}]}}}}]]])

(def rest-api
  (ring/ring-handler
    (ring/router
      (routes))))
