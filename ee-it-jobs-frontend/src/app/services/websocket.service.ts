import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { NotificationService } from './notification.service';

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private stompClient: any = null;
  private jobUpdateSubject = new Subject<any>();
  private scrapeProgressSubject = new Subject<any>();
  private userNotificationSubject = new Subject<any>();
  private connected = false;

  constructor(private notificationService: NotificationService) {}

  connect(userEmail?: string): void {
    if (this.connected) return;
    try {
      const SockJS = (window as any)['SockJS'];
      const Stomp = (window as any)['StompJs'] || (window as any)['Stomp'];
      if (!SockJS || !Stomp) return;

      const apiUrl = environment.apiUrl || window.location.origin;
      const socket = new SockJS(`${apiUrl}/ws`);
      this.stompClient = Stomp.over(socket);
      this.stompClient.debug = () => {};
      this.stompClient.connect({}, () => {
        this.connected = true;
        this.stompClient.subscribe('/topic/jobs', (msg: any) => {
          const data = JSON.parse(msg.body);
          this.jobUpdateSubject.next(data);
          if (data.type === 'SCRAPE_COMPLETE' && data.newJobs > 0) {
            this.notificationService.showNotification('New Jobs Available', {
              body: `${data.newJobs} new jobs found!`,
              tag: 'new-jobs'
            });
          }
        });
        this.stompClient.subscribe('/topic/scrape-progress', (msg: any) => {
          this.scrapeProgressSubject.next(JSON.parse(msg.body));
        });

        // Subscribe to user-specific notifications
        if (userEmail) {
          this.stompClient.subscribe(`/user/${userEmail}/queue/notifications`, (msg: any) => {
            const data = JSON.parse(msg.body);
            this.userNotificationSubject.next(data);
            this.notificationService.showNotification(data.title, {
              body: data.message,
              tag: data.type
            });
          });
        }
      });
    } catch (e) {
      // WebSocket not available, silently fail
    }
  }

  onJobUpdate(): Observable<any> {
    return this.jobUpdateSubject.asObservable();
  }

  onScrapeProgress(): Observable<any> {
    return this.scrapeProgressSubject.asObservable();
  }

  onUserNotification(): Observable<any> {
    return this.userNotificationSubject.asObservable();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.connected = false;
    }
  }

  ngOnDestroy() {
    this.disconnect();
  }
}
