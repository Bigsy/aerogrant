# aerogrant

A Clojure library which pulls together integrant and aero for config nirvana 
https://www.pixelated-noise.com/blog/2022/04/28/integrant-and-aero/index.html

Also throws in the ability of pulling secrets from AWS secrets messenger

## Usage

You can reference integrant but using #ig/ref this allows you to build the intgrant 
app objects but running (aero.core/read-config)

In addition to allowing a mix aero config with building integrant you can also pull secrets
from aws secrets manager using the reader macro of 
``` #asm["secret-location# key]```

By default it will pull aws credentials  by the default aws java client chain but these can be overriden 
like so ```#asm["dev/wibble/test" "wibblek" #ref [:app :project] #ref [:app :project-region]]```

