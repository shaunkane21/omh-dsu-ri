import { Component, OnInit } from '@angular/core';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import { ApiService } from '../api.service';
import { MatChipInputEvent } from '@angular/material/chips';
import * as CryptoJS from 'crypto-js';
import { KeysService } from '../keys.service';
import { stringify } from 'querystring';
import { analyzeAndValidateNgModules } from '@angular/compiler';
export interface Fruit {
  name: string;
}

@Component({
  selector: 'app-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css']
})
export class UploadComponent implements OnInit {
  afuConfig: any;
  api_response: any;
  selected_file: File;
  uploaded_data = [];
  selectable = true;
  removable = true;
  encryptionOutput: string;
  addOnBlur = true;
  readonly separatorKeysCodes = [ENTER, COMMA] as const;
  fruits: Fruit[] = [];
  constructor(private api: ApiService, private keys: KeysService) { }

  ngOnInit(): void {
  }


  onFileChanged(event)
  {
    
    this.selected_file = event.target.files[0];
    const fileReader = new FileReader();
    fileReader.readAsText(this.selected_file, "UTF-8");
    fileReader.onloadend = (e) => {
      const arr = fileReader.result.toString().replace(/\r\n/g, '\n').split('\n');
      for (let i of arr)
      {
        this.uploaded_data.push(i);
        
      }
      
    }
    
    fileReader.onerror = (error) => {
      console.log(error);
    }
  }

  add(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();

    // Add our fruit
    if (value) {
      this.fruits.push({name: value});
    }

    // Clear the input value
    // event.chipInput!.clear();
  }

  remove(fruit: Fruit): void {
    const index = this.fruits.indexOf(fruit);

    if (index >= 0) {
      this.fruits.splice(index, 1);
    }
  }

  AESencryptAndUpload(): void {
    console.log(this.uploaded_data.toString());
    this.encryptionOutput = CryptoJS.AES.encrypt(this.uploaded_data.toString().trim(), this.keys.getAESKey());
    console.log(this.encryptionOutput);
    var val = this.encryptionOutput['ciphertext'].toString();
    var data = {"value": val};
    console.log("start upload" + data);
    this.api.postEncryptedData(data).subscribe((data: any) => {
      console.log("GUID: " + data.result.GUID);
      var guid = data.result.GUID.toString();
      console.log(guid);
      var k = this.keys.getAESKey();
      console.log(k);
      console.log(this.fruits);
      var attrs = [];
      for (var idx in this.fruits)
      {
        var tmp = this.fruits[idx];
        console.log(tmp.name);
        var name = tmp.name;
        attrs[idx] = tmp.name;
      }
      var key = attrs.toString();
      console.log(key);
      var val = {"GUID": guid, "k": k};
      var dataVal = val.toString();
      var abeData = {
        "key": key,
        "value": dataVal
      };
      console.log(abeData);
      this.api.postABEEncryptionInfo(abeData).subscribe((data:any) => {
        this.keys.setAttrList(this.fruits);
        console.log("abe post" + data.result);
        var mHealth = {
          'id' : guid.toString(),
          'key': k.toString()
        };
        
        // this.api.postKeys(guid).subscribe((data: any) => {
        //   console.log("finish upload" + data);
        // })
      });

    });

  }

}
