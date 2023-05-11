#!/bin/bash
rm -Rf public/bundle*
rm -Rf target/*
yarn install --force && CI=true yarn test && yarn build
