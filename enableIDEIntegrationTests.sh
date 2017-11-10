#!/usr/bin/env bash

#This script will unzip the command and query raml files onto the
# integration-test target folder, so that the Integration Tests can be run from the IDE.

#Copy files
cp sjp-command/sjp-command-api/target/*raml.jar sjp-integration-test/target/ ;
cp sjp-query/sjp-query-api/target/*raml.jar sjp-integration-test/target/ ;

#unzip files
unzip -o sjp-integration-test/target/sjp-command-api-*-raml.jar raml/* -d sjp-integration-test/target/test-classes/;
unzip -o sjp-integration-test/target/sjp-query-api-*-raml.jar raml/* -d sjp-integration-test/target/test-classes/;
