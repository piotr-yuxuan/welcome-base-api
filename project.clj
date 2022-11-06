(defproject com.github.piotr-yuxuan/welcome-base-api (-> "./resources/welcome-base-api.version" slurp .trim)
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
                 [http-kit "2.7.0-alpha1"] ; http server
                 [metosin/reitit "0.5.18" :exclusions [metosin/malli]] ; Router for Clojure(Script)
                 [metosin/malli "0.9.2"]
                 [metosin/ring-http-response "0.9.3" :exclusions [ring/ring-core]] ; No magic numbers for http statuses
                 [prestancedesign/get-port "0.1.1"] ; Get an available TCP port

                 ;; API security, authorization, authentication
                 [ovotech/ring-jwt "2.3.0" :exclusions [cheshire]] ; authentication, authorization based on jwt, exclude vulnerable transitive dependency [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.10.2"]
                 [cheshire "5.11.0"]

                 ;; Kafka and avro
                 [org.apache.kafka/kafka-clients "7.3.0-ce"]
                 [org.apache.avro/avro "1.11.1"]
                 [io.confluent/kafka-avro-serializer "7.3.0"]
                 [piotr-yuxuan/slava "0.33.0" :exclusions [riddley]] ; Avro record manipulation for Clojure

                 ;; Observability, living in production

                 ;; Logging

                 ;; Configuration
                 [com.brunobonacci/oneconfig "0.22.0" :exclusions [metosin/jsonista com.cognitect.aws/api]] ; Exclude vulnerable transitive dependency [org.eclipse.jetty/jetty-io "9.4.24.v20191120"]
                 [com.cognitect.aws/api "0.8.612"] ; Hard-patch vulnerable transitive dependency
                 [piotr-yuxuan/closeable-map "0.36.2"] ; A Clojure map that implements java.io.Closeable
                 [com.github.piotr-yuxuan/malli-cli "2.0.0"] ; Configuration value from the command-line

                 [babashka/process "0.3.11"] ; Clojure wrapper for java.lang.ProcessBuilder
                 [juji/editscript "0.6.2"] ; A diff library for Clojure/ClojureScript data structures
                 [camel-snake-kebab "0.4.3"] ; case manipulation
                 [com.xtdb/xtdb-core "1.22.1" :exclusions [org.clojure/data.json org.clojure/tools.reader]] ; Database

                 ;; Language constructs and utilities
                 [org.clojure/clojure "1.12.0-alpha1"] ; The language itself
                 [org.clojure/core.cache "1.0.225"] ; Caching logic
                 [org.clojure/data.avl "0.1.0"]
                 ]
  :main piotr-yuxuan.welcome-base-api.main
  :profiles {:github {:github/topics ["clojure" "api" "example" "exchange" "finance" "fintech" "market"
                                      "stock-markets" "stock-trading" "trading-systems" "trading-tasks"]
                      :github/private? false}
             :dev {:global-vars {*warn-on-reflection* true}
                   :source-paths ["dev"]
                   :repl-options {:init-ns user, :timeout 1e6}
                   :jvm-opts ["-Djdk.attach.allowAttachSelf"
                              "-XX:+UnlockDiagnosticVMOptions"
                              "-XX:+PreserveFramePointer"
                              "-XX:+DebugNonSafepoints"]
                   :dependencies [[ring/ring-devel "2.0.0-alpha-1" :exclusions [crypto-random commons-io]] ; `wrap-reload` ring middleware
                                  [criterium "0.4.6"] ; Basic performance test. Use clojure-jmh for repeatable mesurements.
                                  [jmh-clojure "0.4.1"] ; Clojure wrapper for Java Microbenchmark Harness.
                                  [com.clojure-goes-fast/clj-async-profiler "1.0.2"] ; Sampling CPU and HEAP profiler for Clojure featuring AsyncGetCallTrace + perf_events
                                  [fipp "0.6.26"] ; Fast Idiomatic Pretty Printer for Clojure
                                  ]}
             :kaocha {:dependencies [[lambdaisland/kaocha "1.71.1119"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.disable-locals-clearing=false"
                                  "-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"test" ["with-profile" "+test,+kaocha" "run" "-m" "kaocha.runner" "--skip-meta" ":perf"] ; Kaocha as default test runner
            "ci-perf-test" ["with-profile" "+test,+kaocha" "run" "-m" "kaocha.runner" "--focus-meta" ":perf"]}
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
