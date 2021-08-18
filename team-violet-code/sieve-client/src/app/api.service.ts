import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  headers = new HttpHeaders({
    'Content-Type': 'application/json',
    'Authorization': 'Bearer 1ead418b-0d6c-4d6c-982d-df6c4cd252e4',
    'Accept': 'application/json' });
  options = { headers: this.headers };
  constructor(private http: HttpClient) { }

  postEncryptedData(data)
  {
    return this.http.post('/api/storage/', data);
  }

  postABEEncryptionInfo(data)
  {
    return this.http.post('/api/storage/ABE', data);
  }

  getRequestors()
  {
    return this.http.get('/api/storage/requestors');
  }

  postKeys(data)
  {
    return this.http.get(`/mHealth/v1.0M1/sieve/${data}`, this.options);
  }
}
