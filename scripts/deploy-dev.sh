lein uberjar
aws s3 sync resources/public/ s3://hiiop-dev --delete
heroku git:remote --app hiiop-dev
heroku maintenance:on --app hiiop-dev
DATABASE_URL="$(heroku config:get "DATABASE_URL" --app hiiop-dev)?sslmode=require" lein migratus migrate
HEROKU_APP="hiiop-dev" lein heroku deploy-uberjar
heroku maintenance:off --app hiiop-dev