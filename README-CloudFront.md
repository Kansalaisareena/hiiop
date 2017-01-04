# CloudFront Setup for hiiop100.fi

## Request Routing and CloudFront

### Route 53
Route 53 maps tiedostot.hiiop100.fi and kuvat.hiiop100.fi to the CloudFront domain name. Tarinat.hiiop100.fi goes directly to S3 bucket since it was not part of the SSL certificate, meaning it’s not part of the CloudFront distribution. Certificate is signed by Symantec and is imported to ACM (we’re not using ACM certificates).

### CloudFront Distributions
There’s two Cloudfront distributions, one for the actual service (hiiop100.fi) which runs in Heroku and another one which is for static files stored in S3 buckets. 

### Behaviours

CloudFront behaviours are used to route the requests to the correct S3 bucket. We have three buckets per environment (scratch, dev and prod), but only prod has a Cloudfront distribution set up. Two of the buckets (hiiop-prod and hiiop-prod-pictures) are used as origins for CloudFront:

Hiiop-prod-pictures => https://tiedostot.hiiop100.fi/images (or https://kuvat.hiiop100.fi/images)
Hiiop-prod => https://tiedostot.hiiop100.fi

### Adding more 
If you want to add more S3 buckets under Cloudfront, you need to
1. Create the bucket to S3 with proper Policy and CORS configuration
2. Add the bucket as a new Origin to Cloudfront
3. Add the Behaviour (rule) for which path/file-pattern should be routed to that bucket. Notice that order matters, CloudFront uses first-match. Also notice that if you use path (f.ex image/*) that path is included in the request (meaning there needs to be folder that matches the path)

More info and details: http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/distribution-web-values-specify.html

## Monitoring & Alarms

You can monitor the CloudFront distribution quite nicely from the CloudFront console.

You can also invalidate the CloudFront cache (with specific filters) from the Invalidations-tab. This could be useful if CloudFront has cached some asset that wasn’t available at the time of the request. 

Default Error caching is 5 mins (4xx’s and 5xx’s)

### CloudFront email alerts
SNS-topic cloudfront-alerts: 
Arn:aws:sns:eu-west-1:134131636614:cloudfront-alerts
Important! This has to be created to N-Virginia region!

Go to CloudWatch > Alarms
You can modify the email-address, alert conditions, etc over here

### CloudFront logs

Logs go to S3 bucket hiiop-logs into folder /cloudfront. Folder contains subfolders S3 and heroku for corresponding CloudFront distributions.

S3 bucket has a lifecycle policy to delete old logs after 365 days.

See more: http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/AccessLogs.html
