#!/bin/bash
rm -Rf public/bundle*
rm -Rf target/*
rm -Rf dist/*
source $NVM_DIR/nvm.sh
nvm use
yarn install --force && CI=true yarn test && yarn build
