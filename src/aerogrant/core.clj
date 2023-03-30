(ns aerogrant.core
  (:require [aero.core :as aero]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cognitect.aws.client.api :as aws]
            [aerogrant.web-ident :as wident]
            [integrant.core :as ig]))

(defn deployment-environment
  ([]
   (let [de (System/getenv "DEPLOYMENT_ENVIRONMENT")] (and (not (string/blank? de)) de)))
  ([config] (get-in config [:app :deployment-environment])))

(defn- get-config-profile
  []
  {:post [(#{:local :dev :prd :cn} %)]}
  (keyword (or (deployment-environment) :local)))

(defn read-config
  ([] (read-config (get-config-profile)))
  ([profile]
   (-> "config.edn"
       io/resource
       (aero/read-config {:profile profile}))))

(defn is-prod-env?
  ([]
   (#{:prd :cn} (deployment-environment)))
  ([env]
   (#{:prd :cn} env)))

(defn sm-client [region profile]
  (try (aws/client {:api                  :secretsmanager
                    :region               (or region
                                              (System/getenv "AWS_REGION")
                                              (System/getProperty "AWS_REGION")
                                              (System/getenv "aws.region")
                                              (System/getProperty "aws.region")
                                              "eu-west-1")
                    :credentials-provider (if profile
                                            (wident/default-credentials-provider profile)
                                            (wident/default-credentials-provider))})
       (catch Exception e (str "issue contacting secrets manager " e))))

(def smc-memo (memoize sm-client))


(defmethod aero/reader 'trim
  [_ _ [value]]
  (when value (string/trim value)))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'asm
  [_ _ [secret key profile region]]

  (let [secrets (:SecretString (aws/invoke (smc-memo region
                                                     profile)
                                           {:op      :GetSecretValue
                                            :request {:SecretId secret}}))]
    (get-in (json/parse-string secrets) [key])))