from Crypto.Cipher import AES
import base64, json, math
from optparse import OptionParser


parser = OptionParser()
parser.add_option("-f", "--file", dest="filename",help="path to json file", metavar="FILE")
parser.add_option("-o", "--outdir",dest="path", help="full path output directory")
parser.add_option("-n", "--name", dest="name",help="name for files", metavar="NAME")
parser.add_option("-e", "--enc", dest="en",help="encrypt", metavar="flag")
parser.add_option("-d", "--dec", dest="dec",help="decrypt", metavar="flag")
(options, args) = parser.parse_args()

jsn=options.filename
outn=options.name
outd=options.path

# AES key must be either 16, 24, or 32 bytes long
COMMON_ENCRYPTION_KEY='asdjk@15r32r1234asdsaeqwe314SEFT'
# Make sure the initialization vector is 16 bytes
COMMON_16_BYTE_IV_FOR_AES="IVIVIVIVIVIVIVIV"

def obj_dict(obj):
	return obj.__dict__

def get_common_cipher():
	return AES.new(COMMON_ENCRYPTION_KEY,AES.MODE_CBC,COMMON_16_BYTE_IV_FOR_AES)

def encrypt_with_common_cipher(cleartext):
	common_cipher = get_common_cipher()
	cleartext_length = len(cleartext)
	nearest_multiple_of_16 = 16 * math.ceil(cleartext_length/16)
	padded_cleartext = cleartext.rjust(nearest_multiple_of_16)
	raw_ciphertext = common_cipher.encrypt(padded_cleartext)
	return base64.b64encode(raw_ciphertext).decode('utf-8')

def decrypt_with_common_cipher(ciphertext):
	common_cipher = get_common_cipher()
	raw_ciphertext = base64.b64decode(ciphertext)
	decrypted_message_with_padding = common_cipher.decrypt(raw_ciphertext)
	return decrypted_message_with_padding.decode('utf-8').strip()

def encrypt_json_with_common_cipher(json_obj):
	json_string = json.dumps(json_obj)
	return encrypt_with_common_cipher(json_string)


def decrypt_json_with_common_cipher(json_ciphertext):
	json_string = decrypt_with_common_cipher(json_ciphertext)
	return json.loads(json_string)

twts = []
for line in open(jsn, 'r'):
	twts.append(json.loads(line))
json_string = json.dumps(twts, default=obj_dict)

if(options.en):
	json_ciphertext = encrypt_json_with_common_cipher(json_string)
	file = open(outn+"_encrypt.txt", "w")
	file.write(json_ciphertext)
	file.close
	#json_ciphertext = encrypt_json_with_common_cipher(json_string)
	decryped_json_obj = decrypt_json_with_common_cipher(json_ciphertext)
	file1 = open(outn+"_decrypt.txt", "w")
	file1.write(decryped_json_obj)
	file1.close

