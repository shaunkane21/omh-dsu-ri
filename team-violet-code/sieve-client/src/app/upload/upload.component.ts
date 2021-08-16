import { Component, OnInit } from '@angular/core';
import { ApiService } from '../api.service';

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
  constructor(private api: ApiService) { }

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
        this.api.postEncryptedData(i);
      }
      
    }
    
    fileReader.onerror = (error) => {
      console.log(error);
    }
  }

}
