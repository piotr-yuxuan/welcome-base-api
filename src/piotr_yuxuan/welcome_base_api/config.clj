(ns piotr-yuxuan.welcome-base-api.config
  (:require [babashka.process :as process]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.brunobonacci.oneconfig :refer [configure deep-merge]]
            [malli.core :as m]
            [piotr-yuxuan.malli-cli :as malli-cli]
            [malli.transform :as mt]
            [malli.error :as me]))

(def service-name "welcome-base-api")

(defn env
  []
  (or (System/getenv "ENV") "local"))

(defn version
  []
  (some->> (io/resource (str service-name ".version"))
           slurp
           str/trim))

(defmacro commit
  []
  (-> (process/process "git rev-parse HEAD" {:out :string})
      process/check
      :out
      str/trim))

(def Config
  (m/schema
    [:map {:closed true ; You don't want to accept deceptive user config that actually does nothing.
           :decode/args-transformer malli-cli/args-transformer}
     [:version [:and string?]]
     [:commit [:string {:min 40 :max 40}]]
     [:env [string? {:description ""
                     :short-option "-e"
                     :default (env)}]]
     [:port [pos-int? {:description ""
                       :short-option "-p"
                       :env-var "PORT"
                       :default 8080}]]]))

(defn from-one-config
  [service-name env version]
  (:value (configure {:key service-name
                      :env env
                      :version version})))

(defn from-cli-args
  [args]
  (m/decode Config args (mt/transformer
                          malli-cli/args-transformer
                          mt/strip-extra-keys-transformer
                          mt/string-transformer)))

(defn valid-config
  [value]
  (when-let [error-explanation (m/explain Config value)]
    (let [humanized-explanation (me/humanize error-explanation)]
      ;; FIXME Hide secrets from logs
      ;; FIXME See https://github.com/piotr-yuxuan/malli-cli/issues/19
      (println humanized-explanation)
      (throw (ex-info "Invalid configuration" humanized-explanation))))
  value)

(defn from-env-vars
  [config-fragment]
  (m/decode Config config-fragment (mt/transformer
                                     (mt/default-value-transformer {:key :env-var, :default-fn #(get (malli-cli/*system-get-env*) %2)})
                                     (mt/string-transformer))))

(defn from-defaults
  [config-fragment]
  (m/decode Config config-fragment (mt/default-value-transformer {:key :default})))

(defn base-config
  []
  {:version (version)
   :commit (commit)})

(defn load-config
  ([] (load-config []))
  ([args]
   (-> (base-config)
       (deep-merge (from-one-config service-name (env) (version))
                   (from-cli-args args))
       from-env-vars
       from-defaults
       valid-config)))
