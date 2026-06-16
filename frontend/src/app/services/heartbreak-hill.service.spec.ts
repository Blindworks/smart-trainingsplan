import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HeartbreakHillService } from './heartbreak-hill.service';

describe('HeartbreakHillService', () => {
  let service: HeartbreakHillService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(HeartbreakHillService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getChallenge GETs the challenge by slug', () => {
    service.getChallenge().subscribe();
    const req = httpMock.expectOne(r =>
      r.method === 'GET' && r.url.endsWith('/public/challenges/heartbreak-hill-2026'));
    expect(req.request.method).toBe('GET');
    req.flush({ slug: 'heartbreak-hill-2026', rideCount: 0, runCount: 0 });
  });

  it('getLeaderboard passes the activity type as a query param', () => {
    service.getLeaderboard('RUN').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/leaderboard') && r.params.get('type') === 'RUN');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('submitEffort POSTs multipart form data', () => {
    const file = new File(['<gpx/>'], 'ride.gpx', { type: 'application/gpx+xml' });
    service.submitEffort('RIDE', 'Lukas', file).subscribe();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/efforts'));
    const body = req.request.body as FormData;
    expect(body.get('displayName')).toBe('Lukas');
    expect(body.get('type')).toBe('RIDE');
    expect(body.get('file')).toBeTruthy();
    expect(req.request.headers.has('Content-Type')).toBe(false);
    req.flush({ effortId: 1, rank: 1 });
  });

  it('getTrack GETs the effort track by id', () => {
    service.getTrack(42).subscribe();
    const req = httpMock.expectOne(r =>
      r.method === 'GET' && r.url.endsWith('/efforts/42/track'));
    expect(req.request.method).toBe('GET');
    req.flush({ effortId: 42, activityType: 'RIDE', trackJson: '[]' });
  });
});
