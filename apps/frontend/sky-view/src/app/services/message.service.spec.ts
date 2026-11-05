import {of} from 'rxjs';
import {MessagesService} from '../api-generated/api/messages.service';
import {MessageDTO} from '../api-generated/model/messageDTO';
import {MessagePage} from '../api-generated/model/messagePage';
import {Message, MessageService} from './message.service';

function messagePage(content: MessageDTO[]): MessagePage {
  return {
    content,
    empty: content.length === 0,
    first: true,
    last: true,
    number: 0,
    numberOfElements: content.length,
    pageable: {
      offset: 0,
      pageNumber: 0,
      pageSize: 500,
      paged: true,
      sort: {empty: true, sorted: false, unsorted: true},
      unpaged: false,
    },
    size: 500,
    sort: {empty: true, sorted: false, unsorted: true},
    totalElements: content.length,
    totalPages: 1,
  };
}

const messageDto: MessageDTO = {
  id: '33333333-3333-3333-3333-333333333333',
  createdTime: '2026-11-05T10:00:00',
  read: false,
  receiverEmail: 'receiver@test.local',
  senderEmail: 'sender@test.local',
  text: 'hello',
};

describe('MessageService', () => {
  it('maps the typed received message page content to messages', () => {
    const getReceivedMessages = jasmine.createSpy('getReceivedMessages').and.returnValue(of(messagePage([messageDto])));
    const messagesService = {getReceivedMessages} as unknown as MessagesService;
    const service = new MessageService(messagesService);

    let messages: Message[] = [];
    service.getReceived().subscribe((result) => (messages = result));

    expect(getReceivedMessages).toHaveBeenCalledOnceWith({size: 500}, 'body', false);
    expect(messages.length).toBe(1);
    expect(messages[0].id).toBe(messageDto.id!);
    expect(messages[0].createdTime).toBe('2026-11-05T10:00:00');
    expect(messages[0].senderEmail).toBe('sender@test.local');
  });

  it('maps the typed sent message page content to messages', () => {
    const getSentMessages = jasmine.createSpy('getSentMessages').and.returnValue(of(messagePage([messageDto])));
    const messagesService = {getSentMessages} as unknown as MessagesService;
    const service = new MessageService(messagesService);

    let messages: Message[] = [];
    service.getSent().subscribe((result) => (messages = result));

    expect(getSentMessages).toHaveBeenCalledOnceWith({size: 500}, 'body', false);
    expect(messages.map((m) => m.text)).toEqual(['hello']);
  });

  it('sends through the generated endpoint with receiver and text', () => {
    const sendMessage = jasmine.createSpy('sendMessage').and.returnValue(of(messageDto));
    const messagesService = {sendMessage} as unknown as MessagesService;
    const service = new MessageService(messagesService);

    let message: Message | undefined;
    service.sendDirect('receiver@test.local', 'hello').subscribe((result) => (message = result));

    expect(sendMessage).toHaveBeenCalledOnceWith(
      {receiverEmail: 'receiver@test.local', text: 'hello'}, 'body', false);
    expect(message!.id).toBe(messageDto.id!);
  });

  it('deletes through the generated endpoint with the message id', () => {
    const deleteMessage = jasmine.createSpy('deleteMessage').and.returnValue(of(undefined));
    const messagesService = {deleteMessage} as unknown as MessagesService;
    const service = new MessageService(messagesService);

    service.deleteMessage(messageDto.id!).subscribe();

    expect(deleteMessage).toHaveBeenCalledOnceWith(messageDto.id!, 'body', false);
  });
});
