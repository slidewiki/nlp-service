# Microservice Template #
// TODO: URL zu richtigen service [![Build Status](https://travis-ci.org/slidewiki/microservice-template.svg?branch=master)](https://travis-ci.org/slidewiki/microservice-template)
// TODO: auf eigenen Service [![License](https://img.shields.io/badge/License-MPL%202.0-green.svg)](https://github.com/slidewiki/microservice-template/blob/master/LICENSE)
[![Language](https://img.shields.io/badge/Language-Java%8.0-lightgrey.svg)](https://en.wikipedia.org/wiki/Java_(software_platform))
[![Framework](https://img.shields.io/badge/Framework-Play%20Framework%202.5.12-blue.svg)](https://www.playframework.com/)
// TODO: URL zum Service[![Coverage Status](https://coveralls.io/repos/github/slidewiki/microservice-template/badge.svg?branch=master)](https://coveralls.io/github/slidewiki/microservice-template?branch=master)

// TODO: Service beschreiben
This repository contains the template code for a NodeJS based Microservice of the Slidewiki 2.0 project. Please do **NOT** clone this repository and develop your application in it. Instead fork it (following the instructions below) and develop your application there. The CI maintainer ([Roy Meissner](https://github.com/rmeissn)) will setup continuous integration and delivery for your fork. Just notify him.

Remember to exchange badge urls when forking!


// TODO bis unten zu Install etc. 
### Install NodeJS ###
---
Please visit the wiki at [**Install NodeJS**](https://github.com/slidewiki/microservice-template/wiki/Install-NodeJS).

### Where to start developing? ###
---
Have a look at the file [application/server.js](https://github.com/slidewiki/Microservice-Template/blob/master/application/server.js), that is the main routine of this service. Follow the **require(...)** statements to get trough the entire code in the right order.

When you want to have a look at **tests**, head over to the folder [application/tests/](https://github.com/slidewiki/Microservice-Template/tree/master/application/tests). We're using Mocha and Chai for our purposes.

Since we're developing our application with NodeJS, we're using [npm](https://docs.npmjs.com/) as a **task runner**. Have a look at the [/application/package.json](https://github.com/slidewiki/Microservice-Template/blob/master/application/package.json) script section to obtain an overview of available commands. Some are:

```
# Run syntax check and lint your code
npm run lint

# Run unit tests
npm run unit:test

# Start the application
npm start
...
```

You want to **checkout this cool service**? Simply start the service and head over to: [http://localhost:3000/documentation](http://localhost:3000/documentation). We're using  [swagger](https://www.npmjs.com/package/hapi-swagger) to have this super cool API discrovery/documentation tool. BTW.: Did you already discoverd the super easy swagger integration inside [/application/routes.js](https://github.com/slidewiki/Microservice-Template/blob/master/application/routes.js)? Tags 'api' and 'description' were everything we needed to add.

### What's about Continuous Integration/Delivery? ###
---
Continuous Integration (and in the future Continuous Delivery) is currently setup by using the (for OSS projects) free to use web application [Snap-CI](https://snap-ci.com/). By clicking on the first badge (see at the top), you will be redirected to Snap-CI. There you can have a look at all the different build stages.

We've also setup Code Coverage reports. This is done by [Coveralls](https://coveralls.io). Just click on the coverage badge and you'll be redirected to our corresponding Coveralls project.

In the future, we will exchange Snap-CI with our local instance of Bamboo at the Fraunhofer institute. There will be no changes for you, except that the UI looks a little different.

### Use Docker to run/test your application ###
---
You can use [Docker](https://www.docker.com/) to build, test and run your application locally. Simply edit the Dockerfile and run:

```
docker build -t MY_IMAGE_TAG ./
docker run -it --rm -p 8880:3000 MY_IMAGE_TAG
```

Alternatively you can use [docker-compose](https://docs.docker.com/compose/) to run your application in conjunction with a (local) mongodb instance. Simply execute:

```
docker-compose up -d
```
