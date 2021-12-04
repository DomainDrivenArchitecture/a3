(defproject asn1-parser "0.1.0-SNAPSHOT"
  :description "an asn.1 parser"
  :url "https://domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]]
  :target-path "target/%s/"
  :source-paths ["src/main/clj"]
  :resource-paths ["src/main/resources"]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:jdk11{:dependencies [[javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]]}
             :test {:test-paths ["src/test/clj"]
                    :resource-paths ["src/test/resources"]
                    :dependencies [[dda/data-test "0.1.1"]]}})