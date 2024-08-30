(defproject org.clojars.bigsy/aerogrant "0.1.8"
  :description "mashup of aero and integrant with some nice addons"
  :url "https://github.com/Bigsy/aerogrant"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [aero "1.1.6"]
                 [cheshire "5.13.0"]
                 [com.cognitect.aws/api "0.8.692"]
                 [com.cognitect.aws/sts "857.2.1574.0"]
                 [com.cognitect.aws/endpoints "1.1.12.718"]
                 [com.cognitect.aws/secretsmanager "868.2.1599.0"]
                 [integrant "0.10.0"]]
  :repl-options {:init-ns aerogrant.core})
