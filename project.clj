(defproject piotr-yuxuan/welcome-base-api (-> "./resources/welcome-base-api.version" slurp .trim)
  :description "Common idioms, tools, and code structures for an API"
  :url "https://github.com/piotr-yuxuan/welcome-base-api"
  :license {:name "European Union Public License 1.2 or later"
            :url "https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12"
            :distribution :repo}
  :scm {:name "git"
        :url "https://github.com/piotr-yuxuan/welcome-base-api.git"}
  :pom-addition [:developers [:developer
                              [:name "胡雨軒 Петр"]
                              [:url "https://github.com/piotr-yuxuan"]]]
  :dependencies [;; Core business domain

                 ;; HTTP
                 [aleph "0.4.7-rc3"] ; http server
                 [metosin/reitit "0.5.16"] ; Router for ClojureScript
                 [metosin/ring-http-response "0.9.3" :exclusions [ring/ring-core]] ; No magic numbers for http statuses
                 [prestancedesign/get-port "0.1.1"] ; Get an available TCP port
                 ;[metosin/jsonista "0.3.1"] ; json parsing
                 ;[metosin/muuntaja "0.6.8"] ; content negociation
                 ;[metosin/reitit "0.5.12"] ; router
                 ;[metosin/ring-http-response "0.9.2"] ; http statuses
                 ;[ovotech/ring-jwt "2.2.1"] ; authentication, authorization based on jwt

                 ;; Kafka and avro
                 [org.apache.kafka/kafka-clients "2.5.0"]
                 [org.apache.avro/avro "1.9.2"]
                 [io.confluent/kafka-avro-serializer "5.5.0"]
                 [slava "0.0.29" :exclusions [io.confluent/kafka-avro-serializer org.apache.avro/avro]] ; avro record manipulation

                 ;; Observability, living in production

                 ;; Logging

                 ;; Configuration
                 [com.brunobonacci/oneconfig "0.21.0" :exclusions [metosin/jsonista]]
                 [piotr-yuxuan/closeable-map "0.35.0"] ; A Clojure map that implements java.io.Closeable
                 [com.github.piotr-yuxuan/malli-cli "1.0.3"] ; Configuration value from the command-line

                 ;; language constructs and utilities
                 [org.clojure/clojure "1.11.0-rc1"] ; The language itself.
                 [org.clojure/core.cache "1.0.207"] ; Caching logic
                 [babashka/process "0.1.1"] ; Clojure wrapper for java.lang.ProcessBuilder
                 [camel-snake-kebab "0.4.2"] ; case manipulation
                 [juji/editscript "0.5.8"] ; A diff library for Clojure/ClojureScript data structures
                 ]
  :aot :all
  :profiles {:github {:github/topics ["clojure" "api" "example"]}
             :dev {:global-vars {*warn-on-reflection* true}
                   :dependencies [[ring/ring-devel "2.0.0-alpha-1"] ; wrap-reload ring middleware
                                  ]}
             :jar {:jvm-opts ["-Dclojure.compiler.disable-locals-clearing=false"
                              "-Dclojure.compiler.direct-linking=true"]}}
  :deploy-repositories [["clojars" {:sign-releases false
                                    :url "https://clojars.org/repo"
                                    :username :env/WALTER_CLOJARS_USERNAME
                                    :password :env/WALTER_CLOJARS_PASSWORD}]
                        ["github" {:sign-releases false
                                   :url "https://maven.pkg.github.com/piotr-yuxuan/welcome-base-api"
                                   :username :env/GITHUB_ACTOR
                                   :password :env/WALTER_GITHUB_PASSWORD}]]
  :repositories [["slava" {:url "https://maven.pkg.github.com/piotr-yuxuan/slava"
                           :username :env/GH_PACKAGES_USR
                           :password :env/GH_PACKAGES_PSW}]
                 ["packages.confluent.io" {:url "https://packages.confluent.io/maven/"}]])
