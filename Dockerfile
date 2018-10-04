FROM clojure:lein-2.8.1-alpine

RUN apk update && \
    apk upgrade && \
    apk add git

ADD . /hiiop/

WORKDIR /hiiop