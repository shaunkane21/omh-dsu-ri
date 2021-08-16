import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { UploadComponent } from './upload/upload.component';
import { PolicyGenComponent } from './policy-gen/policy-gen.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatTabsModule } from '@angular/material/tabs';
import {MatToolbarModule } from '@angular/material/toolbar';
import { KeysComponent } from './keys/keys.component';
import { AngularFileUploaderModule } from "angular-file-uploader";

@NgModule({
  declarations: [
    AppComponent,
    UploadComponent,
    PolicyGenComponent,
    KeysComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatTabsModule,
    MatToolbarModule,
    AngularFileUploaderModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
