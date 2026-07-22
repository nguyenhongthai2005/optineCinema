import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import movieService from '../services/movie.service';

const FALLBACK_POSTER = 'https://placehold.co/400x600/111827/f8fafc?text=Opticine';

const ageColor = (ageRating) => {
  if (ageRating === 'P') return '#16a34a';
  if (ageRating === 'C18' || ageRating === 'C16') return '#dc2626';
  return '#ea580c';
};

export default function NowShowing() {
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [trailer, setTrailer] = useState(null);

  useEffect(() => {
    movieService.getNowShowing()
      .then((res) => setMovies(res.data || []))
      .catch((err) => {
        console.error('Cannot load now-showing movies', err);
        setError('Không thể tải danh sách phim từ backend. Vui lòng kiểm tra API.');
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <section className="py-5">
      <div className="container-fluid" style={{ maxWidth: 1440, margin: '0 auto', padding: '0 48px' }}>
        <div className="d-flex justify-content-between align-items-end mb-5">
          <div>
            <h2 className="section-title">Phim Đang Chiếu</h2>
            <div className="section-underline"></div>
          </div>
          <Link to="/showtimes" className="see-all-link text-decoration-none">
            Xem tất cả <span className="material-symbols-outlined ms-1">arrow_forward</span>
          </Link>
        </div>

        {loading && <p style={{ color: 'var(--on-surface-variant)' }}>Đang tải phim...</p>}
        {error && <div className="alert alert-danger">{error}</div>}
        {!loading && !error && movies.length === 0 && (
          <div className="text-center py-5" style={{ color: 'var(--on-surface-variant)' }}>
            Chưa có phim đang chiếu.
          </div>
        )}

        <div className="row g-4 row-cols-2 row-cols-md-3 row-cols-lg-5">
          {movies.map((movie) => (
            <div className="col" key={movie.id}>
              <div className="movie-card">
                <img src={movie.posterUrl || FALLBACK_POSTER} alt={movie.title} />
                <div className="movie-card-overlay movie-card-gradient"></div>
                <div className="movie-card-badges">
                  {movie.ageRating && (
                    <span className="age-badge" style={{ backgroundColor: ageColor(movie.ageRating) }}>
                      {movie.ageRating}
                    </span>
                  )}
                  {movie.duration && (
                    <span className="rating-badge mt-1">{movie.duration} phút</span>
                  )}
                </div>
                <div className="movie-card-info">
                  <h3 className="movie-card-title">{movie.title}</h3>
                  <p className="movie-card-genre mb-0">{movie.genre || movie.status || 'NOW_SHOWING'}</p>
                  {movie.trailerEmbedUrl && (
                    <button type="button" className="btn-book-card text-decoration-none text-center" style={{ marginBottom: 8, background: 'rgba(255,255,255,0.16)', color: '#f8fafc' }} onClick={() => setTrailer(movie)}>
                      Trailer
                    </button>
                  )}
                  <Link to={`/showtimes?movieId=${movie.id}`} className="btn-book-card text-decoration-none text-center">
                    Đặt vé
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
      {trailer && (
        <div style={modalStyles.overlay} onClick={() => setTrailer(null)}>
          <div style={modalStyles.modal} onClick={(event) => event.stopPropagation()}>
            <div style={modalStyles.header}>
              <h3 style={modalStyles.title}>{trailer.title}</h3>
              <button type="button" style={modalStyles.close} onClick={() => setTrailer(null)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div style={modalStyles.frame}>
              <iframe
                src={trailer.trailerEmbedUrl}
                title="Trailer"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ width: '100%', height: '100%', border: 0 }}
              />
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

const modalStyles = {
  overlay: { position: 'fixed', inset: 0, background: 'rgba(3, 7, 18, 0.72)', backdropFilter: 'blur(4px)', zIndex: 500, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 18 },
  modal: { width: 900, maxWidth: '96vw', background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: '0 25px 70px rgba(0,0,0,0.45)' },
  header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 14 },
  title: { margin: 0, color: 'var(--color-text)', fontSize: 20, fontWeight: 850 },
  close: { border: '1px solid var(--color-border)', background: 'var(--surface-container-high)', color: 'var(--color-text)', borderRadius: 8, width: 36, height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' },
  frame: { aspectRatio: '16 / 9', width: '100%', borderRadius: 8, overflow: 'hidden', background: '#030712' },
};
