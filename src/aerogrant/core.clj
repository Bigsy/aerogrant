(ns aerogrant.core
  (:require [aero.core :as aero]
            [cheshire.core :as json]
            [clojure.string :as string]
            [cognitect.aws.client.api :as aws]
            [aerogrant.web-ident :as wident]
            [integrant.core :as ig]))



(defn sm-client [env]
  (aws/client {:api                  :secretsmanager
               :region               "eu-west-1"
               :credentials-provider (if (#{:local} env)
                                       (wident/default-credentials-provider "sredev")
                                       (wident/default-credentials-provider))}))

(def smc-memo (memoize sm-client))

(defmethod aero/reader 'trim
  [_ _ [value]]
  (when value (string/trim value)))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'asm
  [context _ [secret key]]
  (let [secrets (:SecretString (aws/invoke (smc-memo (:profile context))
                                           {:op      :GetSecretValue
                                            :request {:SecretId secret}}))]
    (get-in (json/parse-string secrets) [key])))

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
