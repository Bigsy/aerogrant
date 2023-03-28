(ns aerogrant.web-ident
  (:require [clojure.java.io :as io]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.client.shared :as shared]
            [cognitect.aws.credentials :as credentials]
            [cognitect.aws.util :as util])
  (:import (java.io File)))

(def ^:private sts-client
  "An AWS STS client using static, empty credentials."
  (aws/client {:api                  :sts
               :credentials-provider (credentials/basic-credentials-provider
                                       {:access-key-id     ""
                                        :secret-access-key ""})}))

(defn- assume-role-with-web-identity
  "Call AssumeRoleWithWebIdentity on the STS service with `role-arn`,
  `session-name`, and `token`, returning the `:Credentials` map if it exists,
  otherwise `nil`. Passing `nil` for `session-name` will cause a session name to
  be automatically generated."
  [role-arn session-name token]
  (let [session-name (or session-name
                         (str "cognitect-aws-api-" (System/currentTimeMillis)))]
    (-> (aws/invoke sts-client {:op      :AssumeRoleWithWebIdentity
                                :request {:RoleArn          role-arn
                                          :RoleSessionName  session-name
                                          :WebIdentityToken token}})
        :Credentials)))

(defn web-identity-credentials-provider
  "Obtains temporary credentials from STS using the AssumeRoleWithWebIdentity
  API call.
  The role to assume is expected to be in the environment variable AWS_ROLE_ARN,
  and the file containing the web identity token to exchange for the temporary
  credentials is expected to be named by the AWS_WEB_IDENTITY_TOKEN_FILE
  environment variable. Will optionally use AWS_ROLE_SESSION_NAME from the
  environment to name the session, or else a session name will be automatically
  generated."
  ([]
   (web-identity-credentials-provider
     (util/getenv "AWS_ROLE_ARN")
     (util/getenv "AWS_ROLE_SESSION_NAME")
     (io/file (util/getenv "AWS_WEB_IDENTITY_TOKEN_FILE"))))
  ([role-arn session-name ^File token]
   (credentials/cached-credentials-with-auto-refresh
     (reify credentials/CredentialsProvider
       (fetch [_]
         (when (and role-arn token (.isFile token) (.canRead token))
           (when-let [creds (assume-role-with-web-identity role-arn
                                                           session-name
                                                           (slurp token))]
             (credentials/valid-credentials
               {:aws/access-key-id (:AccessKeyId creds)}
               :aws/secret-access-key (:SecretAccessKey creds)
               :aws/session-token (:SessionToken creds)
               ::credentials/ttl (credentials/calculate-ttl creds)
               "web identity"))))))))

(defn default-credentials-provider
  "Like [[credentials/default-credentials-provider]], but with the addition of
  [[web-identity-credentials-provider]] in the same position as in the AWS SDK
  for Java's default credential provider chain."
  ([]
   (default-credentials-provider (or (System/getenv "AWS_PROFILE")
                                     (System/getProperty "AWS_PROFILE")
                                     (System/getenv "aws.profile")
                                     (System/getProperty "aws.profile")
                                     "default")))
  ([profileoverride]
   (credentials/chain-credentials-provider
     [(credentials/environment-credentials-provider)
      (credentials/system-property-credentials-provider)
      (web-identity-credentials-provider)
      (credentials/profile-credentials-provider profileoverride)
      (credentials/container-credentials-provider (shared/http-client))
      (credentials/instance-profile-credentials-provider (shared/http-client))])))

