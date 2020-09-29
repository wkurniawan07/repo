import { Component, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorReportService } from '../../../services/error-report.service';
import { StatusMessageService } from '../../../services/status-message.service';
import { ErrorReportRequest } from '../../../types/api-request';
import { ErrorMessageOutput } from '../../error-message-output';

/**
 * Error report component.
 */
@Component({
  selector: 'tm-error-report',
  templateUrl: './error-report.component.html',
  styleUrls: ['./error-report.component.scss'],
})
export class ErrorReportComponent implements OnInit {

  errorMessage: string = '';
  subject: string = 'User-submitted Error Report';
  content: string = '';
  requestId: string = '';
  sendButtonEnabled: boolean = true;
  errorReportSubmitted: boolean = false;

  constructor(private errorReportService: ErrorReportService,
              private ngbActiveModal: NgbActiveModal,
              private statusMessageService: StatusMessageService) {}

  ngOnInit(): void {
  }

  /**
   * Sends the error report.
   */
  sendErrorReport(): void {
    const request: ErrorReportRequest = {
      requestId: this.requestId,
      subject: this.subject,
      content: this.content,
    };

    this.sendButtonEnabled = false;
    this.errorReportService.sendErrorReport({ request }).subscribe(() => {
      this.errorReportSubmitted = true;
      this.statusMessageService.showSuccessToast('Your error report has been successfully sent');
      this.ngbActiveModal.close();
    }, (res: ErrorMessageOutput) => {
      this.sendButtonEnabled = true;
      this.statusMessageService.showErrorToast(res.error.message);
    });
  }

}
