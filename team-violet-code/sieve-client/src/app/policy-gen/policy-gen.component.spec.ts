import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PolicyGenComponent } from './policy-gen.component';

describe('PolicyGenComponent', () => {
  let component: PolicyGenComponent;
  let fixture: ComponentFixture<PolicyGenComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PolicyGenComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PolicyGenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
