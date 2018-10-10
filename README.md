# hiiop

## TODO Overview

## Dependencies

- PostgreSQL
- Redis
- sassc
- leiningen
- docker

## Development setup

- Install docker
- Set up `profiles.clj` file mention in [Secrets section](#Secrets)
- Run `docker-compose up`
- Open [localhost:3000](http://localhost:3000) in your browser to check if the development environement is ready

## Secrets

   Create profiles.clj under your git root to place the secrets.

  ```clojure
   {:profiles/dev
    {:env
     {:database-url "postgresql://postgres/hiiop_dev?user=postgres"
      :timezone "Europe/Helsinki"
      :site-base-url ""

      :sender-address  ""
      :smtp-server     ""
      :smtp-port       ""
      :smtp-user       ""
      :smtp-password   ""

      :redis
      {:uri "redis://redis:6379" :host "redis" :port 6379}
      :hiiop-pictures-bucket
      "hiiop-dev-pictures"
      :hiiop-pictures-bucket-base-url
      "http://hiiop-dev-pictures.s3-website-eu-west-1.amazonaws.com"
      :aws-access-key-id
      ""
      :aws-secret-access-key
      ""
      :contentful
      {:cd-api-key ""
       :space-id ""
       :webhook-user ""
       :webhook-password ""
       }
      }
      :hiiop-blog-bucket "hiiop-dev-blog"
      :hiiop-bucket "hiiop-dev"
      :hiiop-blog-base-url "http://hiiop-dev-blog.s3-website.eu-central-1.amazonaws.com"

      :analytics-script "script src"

      ;; these are used for testing static site generator in
      ;; local development
      ;; :asset-base-url "http://localhost:3000"
      ;; :hiiop-blog-base-url "http://hiiop-local-dev-blog.s3-website-eu-west-1.amazonaws.com"
      ;; :hiiop-blog-bucket "hiiop-local-dev-blog"

      :social
      {:facebook-app-id "180814445730558"
       :twitter-account "hiiop100"
       :twitter-site "https://www.hiiop100.fi"}
      }}
    :profiles/test
    {:env
     {:database-url "postgresql://localhost/hiiop_test?user=youruser"
      :timezone "Europe/Helsinki"}}}}
  ```

### Dev environment authentication

To add http basic auth for dev environment, add the following to
the env:

```clojure
:http-simple-credentials {:username "dev-username" :password dev-password"}
```

## Migrations

Manage migrations with the following commands:

- apply migration

  `lein migratus migrate`

- run down for previous migration

  `lein migratus rollback`

- create new migration

  `lein migratus create <migration_name>`

[more about migratus](https://github.com/yogthos/migratus)

## Running tests

You can run all the tests from the command line with the =lein
test= but it's horribly slow. A faster way to run tests is to run
them from the repl in the following way:

```clojure
(require '[clojure.test :refer [run-tests]])
(require 'hiiop.test.specific.test)
(run-tests 'hiiop.test.specific.test)
```

## Notice

### Translations

When dealing with translations you have reload the files manually
in your clj repl to see the changes:

```clojure
  (use 'hiiop.translate :reload)
  (restart)
```

## Deployments

- Done in Docker container
  - After running `docker-compose up`, we can go into the web container:
  `docker exec -it hiiop_web_1 bash`
  - Login to heroku using `heroku login`
  - Set `AWS_ACCESS_KEY_ID` and  `AWS_SECRET_ACCESS_KEY` for aws and deploy using deploy files to deploy to equivalance servers. E.g. one liner `AWS_ACCESS_KEY_ID=YOUR_KEY_ID AWS_SECRET_ACCESS_KEY=YOUR_SECRET ./scripts/deploy-scratch.sh` to deploy to scratch server.

- App itself is hosted in Heroku
  - =HEROKU_APP= environment variable used to define where to deploy
  - =DATABASE_URL= is used to determine the database and user to use
  - =ASSET_BASE_URL= is used to determine which URL to use before the assets
  - =HIIOP_PICTURES_BUCKET= is used as S3 file upload target
  - =HIIOP_PICTURES_BUCKET_BASE_URL= is used to refer to the uploaded pictures

- ASSETS are hosted in S3
  - =HIIOP_ASSET_BUCKET= environment variable is used to determine
    which bucket to use
  - =AWS_ACCESS_KEY_ID= and =AWS_SECRET_ACCESS_KEY= environment
    variables can be used to define the user used to authenticate to AWS
  - Git revision is used to version the assets
