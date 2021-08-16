import { Injectable } from '@angular/core';
import { HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private http: HttpClient) { }

  postEncryptedData(data)
  {
    console.log("api called");
    return this.http.post('/api/symmetric/', data);
  }
}
