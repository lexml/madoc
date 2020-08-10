import { Confirm } from './confirm';
import { Component, ViewChild } from '@angular/core';
import { ModalDirective, ModalModule } from 'ngx-bootstrap';


@Component({
  selector: 'app-confirm-dialog',
  template: `
<div bsModal #childModal="bs-modal" ModalModule (onHidden)="onHidden()" [config]="{backdrop: 'static'}"
    class="modal fade" tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" aria-label="Close" (click)="onCancel()">
          <span aria-hidden="true">&times;</span>
        </button>
        <h4 class="modal-title">{{params.title}}</h4>
      </div>
      <div class="modal-body">

        <p style="padding: 15px;">
          {{params.text}}
        </p>

        <span *ngFor="let b of params.buttons;let i = index">
          <button
            type="submit"
            class="btn btn-primary"
            [ngClass]="{'btn-primary': i == 0, 'btn-default': i > 0}"
            (click)="onSelect(b)">
            {{b}}
          </button>
        </span>

      </div>
    </div>
  </div>
</div>
  `,
  styles: ['button { margin-right: 5px;}']
})
export class ConfirmDialogComponent {

  public params: Confirm  = new Confirm();

  public selected: string;

  @ViewChild('childModal', { static: true }) childModal: ModalDirective;

  constructor() { }

  confirm(params: Confirm) {
    this.params = params;
    this.childModal.config = {backdrop: 'static'};
    this.childModal.show();
  }

  onSelect(button: string) {
    this.selected = button;
    this.childModal.hide();
  }

  onCancel() {
    this.childModal.hide();
  }

  onHidden() {
    this.params.callback(this.selected);
  }

}
