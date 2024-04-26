(ns aerogrant.core
  (:require [aero.core :as aero]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cognitect.aws.client.api :as aws]
            [aerogrant.web-ident :as wident]
            [integrant.core :as ig]))

(defn read-file-config [filename profile]
   (-> filename
       io/resource
       (aero/read-config {:profile profile})))

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

(defmethod aero/reader 'ig/refset
  [_ _ value]
  (ig/refset value))

(defmethod aero/reader 'asm
  [_ _ [secret key region]]

  (let [secrets (:SecretString (aws/invoke (sm-client region)
                                           {:op      :GetSecretValue
                                            :request {:SecretId secret}}))]
    (try
      ;; Attempt to parse the string as JSON
      (if key
        (get-in (json/parse-string secrets) [key])
        (json/parse-string secrets))
      ;; If JSON parsing fails, catch the exception and just return the original string
      (catch Exception e
        secrets))))