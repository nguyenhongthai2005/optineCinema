import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import movieService from '../services/movie.service';

const FALLBACK_POSTER = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1400&q=80';

export default function Hero() {
  const navigate = useNavigate();
  const [movies, setMovies] = useState([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [imageFailed, setImageFailed] = useState({});
  const [trailer, setTrailer] = useState(null);

  useEffect(() => {
    let mounted = true;
    movieService.getHotMovies()
      .then((res) => {
        if (!mounted) return;
        setMovies(Array.isArray(res.data) ? res.data : []);
      })
      .catch(() => {
        if (mounted) setMovies([]);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    if (movies.length <= 1) return undefined;
    const timer = setInterval(() => {
      setActiveIndex((index) => (index + 1) % movies.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [movies.length]);

  const activeMovie = movies[activeIndex] || null;
  const heroImage = useMemo(() => {
    if (!activeMovie || imageFailed[activeMovie.id]) return FALLBACK_POSTER;
    return activeMovie.posterUrl || FALLBACK_POSTER;
  }, [activeMovie, imageFailed]);

  const goToShowtimes = () => {
    if (activeMovie?.id) {
      navigate(`/showtimes?movieId=${activeMovie.id}`);
    } else {
      navigate('/showtimes');
    }
  };

  const shortDescription = (value) => {
    if (!value) return 'Khám phá những bộ phim đang chiếu nổi bật nhất tại Opticine.';
    return value.length > 180 ? `${value.slice(0, 177)}...` : value;
  };

  if (!loading && movies.length === 0) {
    return (
      <section className="hero-section">
        <div className="hero-bg">
          <img src={FALLBACK_POSTER} alt="Opticine cinema" />
          <div className="hero-overlay-side hero-gradient"></div>
          <div className="hero-overlay-bottom"></div>
        </div>
        <div className="hero-content">
          <div style={{ maxWidth: 672, zIndex: 10 }}>
            <span className="badge-now-showing">Phim nổi bật</span>
            <h1 className="hero-title" style={{ marginTop: 16 }}>Chưa có phim nổi bật</h1>
            <p className="hero-desc">Các phim đang chiếu nổi bật sẽ xuất hiện tại đây khi được cập nhật trong hệ thống.</p>
            <button type="button" onClick={() => navigate('/showtimes')} className="btn-primary-hero">
              <span className="material-symbols-outlined">confirmation_number</span>
              XEM LỊCH CHIẾU
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="hero-section">
      <div className="hero-bg">
        <img
          src={heroImage}
          alt={activeMovie?.title || 'Phim nổi bật Opticine'}
          onError={() => activeMovie && setImageFailed((prev) => ({ ...prev, [activeMovie.id]: true }))}
        />
        <div className="hero-overlay-side hero-gradient"></div>
        <div className="hero-overlay-bottom"></div>
      </div>

      <div className="hero-content hero-content-hot">
        <div className="hero-copy">
          <div className="d-flex align-items-center gap-3 mb-3 flex-wrap">
            <span className="badge-now-showing">Đang chiếu nổi bật</span>
            {activeMovie?.popularityScore != null && (
              <span className="d-flex align-items-center text-secondary">
              <span
                className="material-symbols-outlined me-1"
                  style={{
                  fontSize: 18,
                      fontVariationSettings: "'FILL' 1, 'wght' 700, 'GRAD' 0, 'opsz' 20"
              }}
>
local_fire_department
</span>                <span style={{ fontSize: 14, fontWeight: 700 }}>{activeMovie.popularityScore}/100 độ hot</span>
              </span>
            )}
          </div>

          <h1 className="hero-title">{loading ? 'ĐANG TẢI...' : activeMovie?.title}</h1>

          <div className="hero-meta">
            {activeMovie?.genre && <span>{activeMovie.genre}</span>}
            {activeMovie?.durationMinutes && <span>{activeMovie.durationMinutes} phút</span>}
            {activeMovie?.ageRating && <span>{activeMovie.ageRating}</span>}
          </div>

          <p className="hero-desc">{shortDescription(activeMovie?.description)}</p>

          <div className="d-flex align-items-center gap-3 flex-wrap">
            <button type="button" onClick={goToShowtimes} className="btn-primary-hero">
              <span className="material-symbols-outlined">confirmation_number</span>
              ĐẶT VÉ
            </button>
            {activeMovie?.trailerEmbedUrl ? (
              <button type="button" className="btn-secondary-hero" onClick={() => setTrailer(activeMovie)}>
                <span className="material-symbols-outlined">play_circle</span>
                XEM TRAILER
              </button>
            ) : (
              <button type="button" className="btn-secondary-hero" disabled style={{ opacity: 0.55, cursor: 'not-allowed' }}>
                <span className="material-symbols-outlined">play_disabled</span>
                CHƯA CÓ TRAILER
              </button>
            )}
          </div>
        </div>

        {activeMovie && (
          <div className="hero-poster-card">
            <img
              src={imageFailed[activeMovie.id] ? FALLBACK_POSTER : activeMovie.posterUrl || FALLBACK_POSTER}
              alt={activeMovie.title}
              onError={() => setImageFailed((prev) => ({ ...prev, [activeMovie.id]: true }))}
            />
          </div>
        )}
      </div>

      {movies.length > 1 && (
        <>
          <button type="button" className="hero-arrow hero-arrow-left" onClick={() => setActiveIndex((activeIndex - 1 + movies.length) % movies.length)} aria-label="Phim trước">
            <span className="material-symbols-outlined">chevron_left</span>
          </button>
          <button type="button" className="hero-arrow hero-arrow-right" onClick={() => setActiveIndex((activeIndex + 1) % movies.length)} aria-label="Phim tiếp theo">
            <span className="material-symbols-outlined">chevron_right</span>
          </button>
          <div className="carousel-indicators-custom">
            {movies.map((movie, index) => (
              <button
                type="button"
                key={movie.id}
                className={index === activeIndex ? 'indicator-active' : 'indicator-inactive'}
                onClick={() => setActiveIndex(index)}
                aria-label={`Chọn phim ${movie.title}`}
              />
            ))}
          </div>
        </>
      )}

      {trailer && (
        <div style={styles.overlay} onClick={() => setTrailer(null)}>
          <div style={styles.modal} onClick={(event) => event.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', marginBottom: 12 }}>
              <div>
                <div style={{ color: 'var(--color-text-muted)', fontSize: 12, fontWeight: 800, textTransform: 'uppercase' }}>Trailer</div>
                <h2 style={{ margin: 0, color: 'var(--color-text)', fontSize: 20 }}>{trailer.title}</h2>
              </div>
              <button type="button" style={styles.close} onClick={() => setTrailer(null)}>
                <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
              </button>
            </div>
            <div style={styles.frame}>
              <iframe
                title={`Trailer ${trailer.title}`}
                src={trailer.trailerEmbedUrl}
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

const styles = {
  overlay: { position: 'fixed', inset: 0, background: 'rgba(3, 7, 18, 0.72)', backdropFilter: 'blur(4px)', zIndex: 500, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 18 },
  modal: { width: 900, maxWidth: '96vw', background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: '0 25px 70px rgba(0,0,0,0.45)' },
  close: { border: '1px solid var(--color-border)', background: 'var(--surface-container-high)', color: 'var(--color-text)', borderRadius: 8, width: 36, height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' },
  frame: { aspectRatio: '16 / 9', width: '100%', borderRadius: 8, overflow: 'hidden', background: '#030712' },
};
