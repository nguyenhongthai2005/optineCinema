import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Button, Badge,
  Spinner, Alert, ButtonGroup, Stack,
} from 'react-bootstrap';
import AppNavbar from '../../components/AppNavbar';
import Footer from '../../components/Footer';
import showtimeService from '../../services/showtime.service';

// ─── Helpers ──────────────────────────────────
const formatDateShort = (dateStr) => {
  const d = new Date(dateStr);
  return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}`;
};

const formatDayLabel = (dateStr, idx) => {
  if (idx === 0) return { main: 'Hôm nay',  sub: formatDateShort(dateStr) };
  if (idx === 1) return { main: 'Ngày mai', sub: formatDateShort(dateStr) };
  const d    = new Date(dateStr);
  const days = ['CN','T2','T3','T4','T5','T6','T7'];
  return { main: days[d.getDay()], sub: formatDateShort(dateStr) };
};

const formatTime = (dt) =>
  new Date(dt).toLocaleTimeString('vi-VN', {
    hour: '2-digit', minute: '2-digit', hour12: false,
  });

const getNext7Days = () =>
  Array.from({ length: 8 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return d.toISOString().split('T')[0];
  });

// ──────────────────────────────────────────────
export default function ShowtimePage() {
  const [searchParams] = useSearchParams();
  const navigate       = useNavigate();

  const movieId = searchParams.get('movieId');
  const days    = getNext7Days();

  const [selectedDate, setSelectedDate] = useState(days[0]);
  const [showtimes, setShowtimes]       = useState([]);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState('');

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await showtimeService.getShowtimes(movieId, selectedDate);
        setShowtimes(res.data);
      } catch (err) {
        console.error('Cannot load showtimes', err);
        setError('Không thể tải lịch chiếu từ backend. Vui lòng kiểm tra API.');
        setShowtimes([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [movieId, selectedDate]);

  // Group theo phim
  const grouped = showtimes.reduce((acc, st) => {
    const key = st.movieId;
    if (!acc[key]) {
      acc[key] = {
        title:     st.movieTitle,
        poster:    st.moviePosterUrl,
        ageRating: st.movieAgeRating,
        duration:  st.movieDurationMinutes,
        items: [],
      };
    }
    acc[key].items.push(st);
    return acc;
  }, {});

  return (
    <>
      <AppNavbar />
      <div style={{ minHeight: '100vh', background: 'var(--surface)', paddingTop: 80 }}>
        <Container style={{ maxWidth: 1000 }} className="py-4">

          {/* Header */}
          <div className="mb-4">
            <h2 className="fw-bold mb-1" style={{ color: 'var(--on-surface)' }}>
              {movieId && showtimes[0]?.movieTitle ? `Lịch chiếu phim: ${showtimes[0].movieTitle}` : 'Lịch chiếu'}
            </h2>
            <p className="mb-0" style={{ color: 'var(--on-surface-variant)', fontSize: 14 }}>
              Chọn ngày và khung giờ phù hợp
            </p>
          </div>

          {/* Chọn ngày */}
          <div className="mb-4" style={{ overflowX: 'auto' }}>
            <ButtonGroup>
              {days.map((day, i) => {
                const label  = formatDayLabel(day, i);
                const active = selectedDate === day;
                return (
                  <Button
                    key={day}
                    variant={active ? 'warning' : 'outline-light'}
                    onClick={() => setSelectedDate(day)}
                    className="px-3 py-2"
                    style={{ minWidth: 84 }}
                  >
                    <div className="fw-bold" style={{ fontSize: 13, lineHeight: 1.2 }}>
                      {label.main}
                    </div>
                    <div style={{ fontSize: 11, opacity: 0.75 }}>{label.sub}</div>
                  </Button>
                );
              })}
            </ButtonGroup>
          </div>

          {/* Loading / error / empty */}
          {loading && (
            <div className="text-center py-5">
              <Spinner animation="border" variant="light" />
            </div>
          )}

          {error && <Alert variant="danger">{error}</Alert>}

          {!loading && !error && Object.keys(grouped).length === 0 && (
            <Card className="text-center py-5 border-0" style={{ background: 'rgba(255,255,255,0.04)' }}>
              <Card.Body>
                <p className="mb-0" style={{ color: 'var(--on-surface-variant)' }}>
                  Hiện chưa có suất chiếu khả dụng.
                </p>
              </Card.Body>
            </Card>
          )}

          {/* Danh sách phim + suất chiếu */}
          {!loading && Object.values(grouped).map((movie, idx) => (
            <Card
              key={idx}
              className="mb-3 border-0"
              style={{ background: 'rgba(255,255,255,0.04)' }}
            >
              <Card.Body>
                <Row className="g-3">

                  {/* Thông tin phim — ẩn nếu đang lọc theo movieId */}
                  {!movieId && (
                    <Col xs={12} md={3}>
                      <Stack direction="horizontal" gap={3} className="align-items-start">
                        {movie.poster && (
                          <img
                            src={movie.poster}
                            alt={movie.title}
                            style={{
                              width: 64, height: 90, objectFit: 'cover',
                              borderRadius: 6, flexShrink: 0,
                            }}
                          />
                        )}
                        <div>
                          <h6 className="fw-bold mb-2" style={{ color: 'var(--on-surface)' }}>
                            {movie.title}
                          </h6>
                          <Stack direction="horizontal" gap={2} className="flex-wrap">
                            {movie.ageRating && (
                              <Badge bg="danger" pill>{movie.ageRating}</Badge>
                            )}
                            {movie.duration && (
                              <small style={{ color: 'var(--on-surface-variant)' }}>
                                {movie.duration} phút
                              </small>
                            )}
                          </Stack>
                        </div>
                      </Stack>
                    </Col>
                  )}

                  {/* Grid suất chiếu */}
                  <Col xs={12} md={movieId ? 12 : 9}>
                    <Row xs={2} sm={3} md={4} className="g-2">
                      {movie.items.map((st) => {
                        const soldOut  = st.availableSeats === 0;
                        const lowSeats = st.availableSeats > 0 && st.availableSeats < 10;
                        return (
                          <Col key={st.id}>
                            <Button
                              variant={soldOut ? 'outline-secondary' : 'outline-light'}
                              disabled={soldOut}
                              onClick={() => navigate(`/showtimes/${st.id}/seats`)}
                              className="w-100 text-start p-2"
                              style={{ minHeight: 92 }}
                            >
                              <div className="fw-bold" style={{ fontSize: 18, letterSpacing: 1 }}>
                                {formatTime(st.startTime)}
                              </div>
                              <div style={{ fontSize: 11, opacity: 0.65 }}>
                                ~ {formatTime(st.endTime)}
                              </div>
                              <Stack direction="horizontal" gap={1} className="mt-1 flex-wrap">
                                {st.screenType && (
                                  <Badge
                                    bg={st.screenType === '3D' ? 'warning' : 'secondary'}
                                    text={st.screenType === '3D' ? 'dark' : 'light'}
                                    style={{ fontSize: 10 }}
                                  >
                                    {st.screenType}
                                  </Badge>
                                )}
                                <Badge
                                  bg={soldOut ? 'secondary' : lowSeats ? 'danger' : 'success'}
                                  style={{ fontSize: 10 }}
                                >
                                  {soldOut ? 'Hết' : `${st.availableSeats} ghế`}
                                </Badge>
                              </Stack>
                              <small className="d-block mt-1" style={{ fontSize: 10, opacity: 0.6 }}>
                                {st.roomName}
                              </small>
                            </Button>
                          </Col>
                        );
                      })}
                    </Row>
                  </Col>
                </Row>
              </Card.Body>
            </Card>
          ))}
        </Container>
      </div>
      <Footer />
    </>
  );
}
