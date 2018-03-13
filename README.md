# NLP Service / Natural Language Processing Service #
[![Build Status](https://travis-ci.org/slidewiki/nlp-service.svg?branch=master)](https://travis-ci.org/slidewiki/nlp-service)
[![License](https://img.shields.io/badge/License-MPL%202.0-green.svg)](https://github.com/slidewiki/nlp-service/blob/master/LICENSE)
[![Language](https://img.shields.io/badge/Language-Java%208.0-lightgrey.svg)](https://en.wikipedia.org/wiki/Java_version_history#Java_8)
[![Framework](https://img.shields.io/badge/Framework-Play%20Framework%202.5.12-blue.svg)](https://www.playframework.com/)
<!--[![Coverage Status](https://coveralls.io/repos/github/slidewiki/microservice-template/badge.svg?branch=master)](https://coveralls.io/github/slidewiki/microservice-template?branch=master)-->

// 
This is a micro service based on Java (via Play framework) which manages the natural language processing (NLP) of the textual content of decks as well as other processes based on these previously calculated NLP results, like tag recommendation or deck recommendation.

The basic NLP service processes the textual content of the deck (by given deck id and by calling the deck service to retrieve the initial html content). The process perfoms different steps, like 
HTMLToText, language detection, tokenization, Named Entity Recognition, DBPediaSpotlight, etc. The result is called the NLP result (containing the process results like detected language, tokens, Named Entities, etc.).

The nlp service contains routes for 
(1) processing decks of SlideWiki:
(a) perform NLP on deck and return NLP result
(b) get tag recommendations for a deck (by indentifying important words and entities in the deck using NLP result of the given deck and statistics of NLP results of all other decks in platform via nlp store - see architecture)

Furthermore, the service additionally contains some convenience routes for
(2) some separate sub processes on textual input (instead of deck id), like language detection, dbpedia spotlight, htmlToText, tokenization

## API ##
see https://nlpservice.experimental.slidewiki.org/docs/

## Architecture ##
(NLP Service and NLP Store Service)
The main architecture is as follows:

Since the calculation of the NLP result takes some time and resources and for later usage we will also need statictics about the NLP results of all decks in the platform, the NLP result of a deck should be stored for later usage. This is done via the nlp store service (see https://nlpstore.experimental.slidewiki.org/documentation). When a deck or slide is added or changed, the nlp store service calls the nlp service to (re-)calculate the NLP result and stores it. If the NLP result is needed later (e.g. for tag recommendation or deck recommendation where the NLP results of lots of/all decks are needed), it can be queried via the nlp store.


##Installation##
The Parameters are set in apllication.conf.template. Via envsubst, some environment variables are set in the apllication.conf

###ENV###
*NLP_APPLICATION_SECRET
Environment variables set via envsubst in application.conf:
*SERVICE_URL_DECK - The service URL for the deck service based on e.g. https://nlpservice.experimental.slidewiki.org
*SERVICE_URL_NLPSTORE _ The service URL for the nlp store service
