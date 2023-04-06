(ns aerogrant.core
  (:require [aero.core :as aero]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cognitect.aws.client.api :as aws]
            [aerogrant.web-ident :as wident]
            [integrant.core :as ig]))

(defn read-config [profile]
   (-> "config.edn"
       io/resource
       (aero/read-config {:profile profile})))

(defn sm-client [region]
  (try (aws/client {:api                  :secretsmanager
                    :region               (or region
                                              (System/getenv "AWS_REGION")
                                              (System/getProperty "AWS_REGION")
                                              (System/getenv "aws.region")
                                              (System/getProperty "aws.region"))
                    :credentials-provider (wident/default-credentials-provider)})
       (catch Exception e (str "issue contacting secrets manager " e))))

(defmethod aero/reader 'trim
  [_ _ [value]]
  (when value (string/trim value)))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'asm
  [_ _ [secret key region]]

  (let [secrets (:SecretString (aws/invoke (sm-client region)
                                           {:op      :GetSecretValue
                                            :request {:SecretId secret}}))]
    (get-in (json/parse-string secrets) [key])))