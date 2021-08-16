from flask_restx import Resource, fields
from flask import request, jsonify
from .main import mongo, api, app
import json
import sys
import bson

storage_space = api.namespace('storage', description="Manage Storage of User Data")
storage_model = api.model('Storage Model', 
	{
		'key': fields.String(required = False,
										description= "GUID or Attributes",
										help="Key cannot be blank"),
		'value': fields.String(required= True, default=False)})



@storage_space.route("/symmetric")
class StorageProviderSymmetric(Resource):
	@api.doc(responses={200: 'OK', 400: 'Invalid Argument', 500: 'Mapping Key Error'})
	def get(self):
		data = mongo.db.data
		output = []
		for n in data.find():
			output.append({'key':n['key'], 'value':n['value']})
		return jsonify({'result':output})
    
    @api.doc(responses={200: 'OK', 400: 'Invalid Argument', 500: 'Mapping Key Error'})
    @api.expect(storage_model)
    def post(self):
        data = request.get_json()
        db = mongo.db.data
        value = request.json['value']
        n_v = {'value': value}
        v_id = db.insert(n_v)
        new_v = db.find_one({'_id':v_id})
        output = {'GUID': new_v['_id']}
        return jsonify({'result': output})

@storage_space.route("/ABE")
class StorageProviderABE(Resource):
    @api.doc(responses={200: 'OK', 400: 'Invalid Argument', 500: 'Mapping Key Error'})
    @api.expect(storage_model)
    def post(self):
        data = request.get_json()
        db = mongo.db.metadata
        key = request.json['key']
        value = request.json['value']
        n_abe = {'key': key, 'value':value}
        a_id = db.insert(n_abe)
        new_abe = db.find_one({'_id':a_id})
        output = {'entry': new_abe['key']}
        return jsonify({'result': output})

@storage_space.route("/ABE/<string:id>")
class StorageProviderABEId(Resource):
    @api.doc(responses={200: 'OK', 400: 'Invalid Argument', 500: 'Mapping Key Error'},
		params={'id': 'ACS'})
    def get(self, id):
        #OpenMHealth to request 
        db = mongo.db.metadata
        output = []
        val = db.find_one({'key': bson.ObjectId(id)})
        output = {'value': val['value']}
        return jsonify({'result': output})

@storage_space.route("/import")
class StorageProviderImport(Resource):
	@api.doc(responses={200: 'OK', 400: 'Invalid Argument', 500: 'Mapping Key Error'})
	def get(self):
		data = mongo.db.requestors
		requestor = {'name': 'openMHealth', 'accessPolicyWritten': 'false'}
		r_id = data.insert(requestor)
        new_r = data.find_one({'_id': r_id})
        output = {'requestor': new_r['name']}
		return jsonify({'result':output})

@storage_space.route("/requestors")
class StorageProviderRequestors(Resource):
	@api.doc(responses={200: 'OK', 400: 'Invalid Argument', 500: 'Mapping Key Error'})
	def get(self):
		data = mongo.db.requestors
		output = []
		for n in data.find():
			output.append({'id': n['_id'], 'name':n['name'], 'accessPolicyWritten':n['accessPolicyWritten']})
		return jsonify({'result':output})


