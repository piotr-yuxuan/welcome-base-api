(ns user
  "Simple functions to start, restart, and stop the server. You need to
  restart when you change configuration. Files in namespaces will
  automatically be reloaded when changed. Of course you still need to
  save them."
  (:require [piotr-yuxuan.welcome-base-api.config :as config]
            [piotr-yuxuan.welcome-base-api.main :as main]
            [reitit.ring :as ring]
            [ring.middleware.reload :as reload])
  (:import (piotr_yuxuan.closeable_map CloseableMap)))

(defonce app
  (atom nil))

(defn handler
  "Indirection made for development purpose. Reload namespaces when
  needed before a responding to the next request."
  [config]
  (ring/ring-handler
    (ring/router [])
    (fn
      ([request] ((main/->handler config) request))
      ([request respond raise] ((main/->handler config) request respond raise)))
    {:middleware [reload/wrap-reload]}))

(defn start
  ([] (start app))
  ([app] (start app (config/load-config)))
  ([app config]
   (when-not @app
     (reset! app (main/config->app handler config)))
   app))

(defn stop
  ([] (stop app))
  ([app]
   (when @app
     (.close ^CloseableMap @app)
     (reset! app nil))
   app))

(defn user-override
  "Shallow function to show how you can override the config when
  you (re)start a server."
  [config]
  ;; Here do what you want to override config.
  config)

(defn restart
  ([] (restart app))
  ([app] (restart app (config/load-config)))
  ([app config]
   (stop app)
   (start app (user-override config))))
