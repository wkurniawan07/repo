import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { NgbCollapseModule, NgbDropdownModule, NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { LoadingRetryModule } from '../../components/loading-retry/loading-retry.module';
import { LoadingSpinnerModule } from '../../components/loading-spinner/loading-spinner.module';
import { PanelChevronModule } from '../../components/panel-chevron/panel-chevron.module';
import { RedirectBannerModule } from '../../components/redirect-banner/redirect-banner.module';
import { SessionsTableModule } from '../../components/sessions-table/sessions-table.module';
import { InstructorHomePageComponent } from './instructor-home-page.component';

const routes: Routes = [
  {
    path: '',
    component: InstructorHomePageComponent,
  },
];

/**
 * Module for instructor home page.
 */
@NgModule({
  declarations: [
    InstructorHomePageComponent,
  ],
  imports: [
    CommonModule,
    SessionsTableModule,
    FormsModule,
    RouterModule.forChild(routes),
    NgbCollapseModule,
    NgbDropdownModule,
    NgbTooltipModule,
    LoadingSpinnerModule,
    LoadingRetryModule,
    PanelChevronModule,
    RedirectBannerModule,
  ],
  exports: [
    InstructorHomePageComponent,
  ],
})
export class InstructorHomePageModule { }
