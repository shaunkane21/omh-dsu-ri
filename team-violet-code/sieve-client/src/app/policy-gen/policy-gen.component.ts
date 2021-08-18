import { Component, OnInit } from '@angular/core';
import { ApiService } from '../api.service';

@Component({
  selector: 'app-policy-gen',
  templateUrl: './policy-gen.component.html',
  styleUrls: ['./policy-gen.component.css']
})
export class PolicyGenComponent implements OnInit {

  constructor(private api: ApiService) { }
  requestors;
  accessPolicy = "";
  ngOnInit(): void {
    this.api.getRequestors().subscribe( res => {
      console.log(res);
      this.requestors = res["result"]
    });
  }

}
