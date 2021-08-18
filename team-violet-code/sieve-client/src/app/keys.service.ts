import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class KeysService {
  public aes_key = "asdjk@15r32r1234asdsaeqwe314SEFT";
  public abe_key = "";
  public attr_list = [];
  constructor() { }

  getAESKey()
  {
    return this.aes_key;
  }

  getABEKey() 
  {
    return this.abe_key;
  }

  setAttrList(list)
  {
    this.attr_list = list;
  }

  getAttrList()
  {
    return this.attr_list;
  }
}
