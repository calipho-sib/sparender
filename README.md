Simple Http Server that renders HTML pages using Selenium.

Used for SEO optimisation (solving problems related to technology such as: AngularJS, webcomponents, polymer)

Run selenium server
```shell 
docker run -d --add-host=www.google-analytics.com:127.0.0.1 --add-host=old.nextprot.org:127.0.0.1 -p 4444:4444 selenium/standalone-chrome
```

Run sparender
```
java -Dehcache.path=/work/sparender/cache -jar app.jar
```


Access your browser at: http://localhost:8082/https://www.nextprot.org/about/human-proteome for an example of plain html file

Configure apache:
```

```


