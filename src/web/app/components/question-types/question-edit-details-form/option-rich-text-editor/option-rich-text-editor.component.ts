import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

/**
 * A rich text editor.
 */
@Component({
  selector: 'tm-option-rich-text-editor',
  templateUrl: './option-rich-text-editor.component.html',
  styleUrls: ['./option-rich-text-editor.component.scss'],
})
export class OptionRichTextEditorComponent implements OnInit {

  @Input()
  isDisabled: boolean = false;

  @Input()
  minHeightInPx: number = 120;

  @Input()
  placeholderText: string = '';

  @Input()
  richText: string = '';

  @Output()
  richTextChange: EventEmitter<string> = new EventEmitter();

  // the argument passed to tinymce.init() in native JavaScript
  init: any = {};

  constructor() { }

  ngOnInit(): void {
    this.init = this.getEditorSettings();
  }

  private getEditorSettings(): any {
    return {
      base_url: '/tinymce',
      skin_url: '/tinymce/skins/ui/oxide',
      suffix: '.min',
      height: this.minHeightInPx,
      resize: false,
      inline: true,
      relative_urls: false,
      convert_urls: false,
      remove_linebreaks: false,
      plugins: [
        'advlist autolink lists link image charmap hr anchor',
        'searchreplace visualblocks visualchars code',
        'insertdatetime nonbreaking save table directionality',
        'emoticons paste textpattern',
      ],
      menubar: false,

      toolbar1: 'styleselect | forecolor backcolor '
          + '| bold italic underline strikethrough subscript superscript '
          + '| alignleft aligncenter alignright alignjustify '
          + '| bullist numlist | link image charmap emoticons',
    };
  }

}
