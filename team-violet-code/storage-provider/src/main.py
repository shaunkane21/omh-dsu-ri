from flask import Flask
from flask_pymongo import PyMongo
from flask_restx import Api, Resource
from flask_cors import CORS
from bson import ObjectId
import json
import datetime
import os
import logging
import sys

class JSONEncoder(json.JSONEncoder):
    ''' extend json-encoder class'''

    def default(self, o):
        if isinstance(o, ObjectId):
            return str(o)
        if isinstance(o, datetime.datetime):
            return str(o)
        return json.JSONEncoder.default(self, o)


app = Flask(__name__)
CORS(app)
app.config["MONGO_URI"] = "mongodb://mongodb:27017/sieve"
# app.config['MONGO3_PORT'] = 27019
mongo = PyMongo(app)

app.json_encoder = JSONEncoder

api = Api(app)


#These imports need to be below the path_planning = PathPlanning() and comms_manager = Communications()
from .storage_provider import StorageProvider


print('Hello world!', file=sys.stderr)
print('This is error output', file=sys.stderr)
print('This is standard output', file=sys.stdout)
app.logger.info('testing info log')

if __name__ == '__main__':

    """ 
        AS OF RIGHT NOW IT DOES NOT LOOK LIKE THIS MAIN FUNCTION EVER GETS HIT
    """

    print('__main__ Hello world!', file=sys.stderr)
    print('__main__ This is error output', file=sys.stderr)
    print('__main__ This is standard output', file=sys.stdout)


    app.run(debug=True)
    app.logger.info('testing info log')