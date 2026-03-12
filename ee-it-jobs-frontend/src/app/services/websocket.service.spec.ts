import { TestBed } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';
import { NotificationService } from './notification.service';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let notifSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    notifSpy = jasmine.createSpyObj('NotificationService', ['showNotification']);

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: NotificationService, useValue: notifSpy }
      ]
    });
    service = TestBed.inject(WebSocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return job update observable', () => {
    const obs = service.onJobUpdate();
    expect(obs).toBeTruthy();
    expect(obs.subscribe).toBeDefined();
  });

  it('should return scrape progress observable', () => {
    const obs = service.onScrapeProgress();
    expect(obs).toBeTruthy();
    expect(obs.subscribe).toBeDefined();
  });

  it('should return user notification observable', () => {
    const obs = service.onUserNotification();
    expect(obs).toBeTruthy();
    expect(obs.subscribe).toBeDefined();
  });

  it('should handle disconnect gracefully when not connected', () => {
    expect(() => service.disconnect()).not.toThrow();
  });

  it('should handle connect gracefully when SockJS not available', () => {
    // SockJS/Stomp not in window in test env - should not throw
    expect(() => service.connect('test@test.com')).not.toThrow();
  });
});
