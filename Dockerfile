FROM clojure:lein-2.8.1-alpine

RUN apk update && \
    apk upgrade && \
    apk add git && \
    apk add sassc && \
    apk add curl && \
    apk add nodejs && \
    apk add py-pip && \
    pip install awscli --upgrade --user && \
    ln -s ~/.local/bin/aws /usr/local/bin/ && \
    curl https://cli-assets.heroku.com/install.sh | sh

ADD . /hiiop/

WORKDIR /hiiop
