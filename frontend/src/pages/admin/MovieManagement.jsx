import { useEffect, useMemo, useState } from 'react'
import adminService from '../../services/admin.service'

const STATUS_LABELS = {
  NOW_SHOWING: 'Đang chiếu',
  COMING_SOON: 'Sắp chiếu',
  STOPPED: 'Ngừng chiếu',
}

const EMPTY_FORM = {
  title: '',
  description: '',
  genre: '',
  durationMinutes: '',
  ageRating: 'P',
  releaseDate: '',
  posterUrl: '',
  trailerUrl: '',
  status: 'NOW_SHOWING',
  popularityScore: 50,
}

const FALLBACK_POSTER = 'https://placehold.co/96x144/111827/f8fafc?text=Opticine'

export default function MovieManagement() {
  const [movies, setMovies] = useState([])
  const [filters, setFilters] = useState({ keyword: '', status: '', genre: '' })
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [trailer, setTrailer] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await adminService.getMovies({
        keyword: filters.keyword || undefined,
        status: filters.status || undefined,
        genre: filters.genre || undefined,
      })
      setMovies(res.data || [])
    } catch (err) {
      setError(errorText(err, 'Không tải được danh sách phim.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(load, 250)
    return () => clearTimeout(timer)
  }, [filters.keyword, filters.status, filters.genre])

  const genreOptions = useMemo(() => {
    return [...new Set(movies.map((movie) => movie.genre).filter(Boolean))].sort()
  }, [movies])

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setEditingId(null)
    setModal('edit')
    setError('')
    setMessage('')
  }

  const openEdit = (movie) => {
    setForm({
      title: movie.title || '',
      description: movie.description || '',
      genre: movie.genre || '',
      durationMinutes: movie.durationMinutes || '',
      ageRating: movie.ageRating || 'P',
      releaseDate: movie.releaseDate || '',
      posterUrl: movie.posterUrl || '',
      trailerUrl: movie.trailerUrl || '',
      status: movie.status || 'NOW_SHOWING',
      popularityScore: movie.popularityScore || 50,
    })
    setEditingId(movie.id)
    setModal('edit')
    setError('')
    setMessage('')
  }

  const update = (key) => (event) => {
    setForm((prev) => ({ ...prev, [key]: event.target.value }))
  }

  const validate = () => {
    if (!form.title.trim()) return 'Tên phim là bắt buộc.'
    if (!Number(form.durationMinutes) || Number(form.durationMinutes) <= 0) return 'Thời lượng phim phải lớn hơn 0.'
    if (!form.status) return 'Trạng thái phim là bắt buộc.'
    if (form.trailerUrl && !extractYouTubeId(form.trailerUrl)) return 'Link trailer YouTube không hợp lệ.'
    if (form.popularityScore && (Number(form.popularityScore) < 1 || Number(form.popularityScore) > 100)) return 'Độ hot phải nằm trong khoảng 1 đến 100.'
    return ''
  }

  const save = async (event) => {
    event.preventDefault()
    const validation = validate()
    if (validation) {
      setError(validation)
      return
    }
    setSaving(true)
    setError('')
    setMessage('')
    const payload = {
      ...form,
      title: form.title.trim(),
      durationMinutes: Number(form.durationMinutes),
      popularityScore: form.popularityScore ? Number(form.popularityScore) : null,
      posterUrl: form.posterUrl || null,
      trailerUrl: form.trailerUrl || null,
      releaseDate: form.releaseDate || null,
    }
    try {
      if (editingId) {
        await adminService.updateMovie(editingId, payload)
        setMessage('Đã cập nhật phim.')
      } else {
        await adminService.createMovie(payload)
        setMessage('Đã tạo phim mới.')
      }
      setModal(null)
      await load()
    } catch (err) {
      setError(errorText(err, 'Không lưu được phim.'))
    } finally {
      setSaving(false)
    }
  }

  const changeStatus = async (movie, status) => {
    setError('')
    setMessage('')
    try {
      await adminService.updateMovieStatus(movie.id, status)
      setMessage(`Đã chuyển "${movie.title}" sang ${STATUS_LABELS[status]}.`)
      await load()
    } catch (err) {
      setError(errorText(err, 'Không cập nhật được trạng thái phim.'))
    }
  }

  const remove = async (movie) => {
    if (!window.confirm(`Ngừng chiếu hoặc xóa phim "${movie.title}"?`)) return
    setError('')
    setMessage('')
    try {
      await adminService.deleteMovie(movie.id)
      setMessage('Đã xử lý phim.')
      await load()
    } catch (err) {
      setError(errorText(err, 'Không thể xóa/ngừng chiếu phim.'))
    }
  }

  const previewTrailer = (movieOrForm) => {
    const videoId = movieOrForm.youtubeVideoId || extractYouTubeId(movieOrForm.trailerUrl)
    if (!videoId) {
      setError(movieOrForm.trailerUrl ? 'Link trailer YouTube không hợp lệ.' : 'Phim chưa có trailer.')
      return
    }
    setTrailer({ title: movieOrForm.title || 'Trailer', embedUrl: `https://www.youtube.com/embed/${videoId}` })
  }

  return (
    <div style={S.page}>
      <div style={S.header}>
        <div>
          <h1 style={S.title}>Quản lý phim</h1>
          <p style={S.subtitle}>Tạo phim, cập nhật poster, trailer YouTube và trạng thái phát hành.</p>
        </div>
        <button type="button" style={S.primaryButton} onClick={openCreate}>
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
          Thêm phim
        </button>
      </div>

      {message && <div style={S.success}>{message}</div>}
      {error && <div style={S.error}>{error}</div>}

      <section style={S.card}>
        <div style={S.filterGrid}>
          <input style={S.input} placeholder="Tìm theo tên hoặc thể loại..." value={filters.keyword} onChange={(e) => setFilters({ ...filters, keyword: e.target.value })} />
          <select style={S.input} value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <option value="">Tất cả trạng thái</option>
            {Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <select style={S.input} value={filters.genre} onChange={(e) => setFilters({ ...filters, genre: e.target.value })}>
            <option value="">Tất cả thể loại</option>
            {genreOptions.map((genre) => <option key={genre} value={genre}>{genre}</option>)}
          </select>
        </div>
      </section>

      <section style={{ ...S.card, marginTop: 16, padding: 0, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={S.table}>
            <thead>
              <tr>{['Poster', 'Tên phim', 'Thể loại', 'Thời lượng', 'Độ tuổi', 'Ngày khởi chiếu', 'Trạng thái', 'Độ hot', 'Thao tác'].map((head) => <th key={head} style={S.th}>{head}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={9} style={{ ...S.td, textAlign: 'center', color: 'var(--color-text-muted)', padding: 36 }}>Đang tải...</td></tr>
              ) : movies.length === 0 ? (
                <tr><td colSpan={9} style={{ ...S.td, textAlign: 'center', color: 'var(--color-text-muted)', padding: 36 }}>Không có phim phù hợp.</td></tr>
              ) : movies.map((movie) => (
                <tr key={movie.id}>
                  <td style={S.td}><Poster src={movie.posterUrl} title={movie.title} /></td>
                  <td style={S.td}>
                    <div style={{ fontWeight: 850, color: 'var(--color-text)', maxWidth: 260 }}>{movie.title}</div>
                    <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>#{movie.id} · {movie.showtimeCount || 0} suất chiếu</div>
                  </td>
                  <td style={S.td}>{movie.genre || '-'}</td>
                  <td style={S.td}>{movie.durationMinutes} phút</td>
                  <td style={S.td}>{movie.ageRating || '-'}</td>
                  <td style={S.td}>{movie.releaseDate || '-'}</td>
                  <td style={S.td}><span style={statusBadge(movie.status)}>{STATUS_LABELS[movie.status] || movie.status}</span></td>
                  <td style={S.td}><strong>{movie.popularityScore || 50}</strong>/100</td>
                  <td style={S.td}>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                      <button type="button" style={S.iconButton} onClick={() => openEdit(movie)} title="Sửa"><span className="material-symbols-outlined">edit</span></button>
                      <button type="button" style={S.iconButton} onClick={() => previewTrailer(movie)} title="Xem trailer"><span className="material-symbols-outlined">play_circle</span></button>
                      {movie.status !== 'STOPPED' && <button type="button" style={S.iconButton} onClick={() => changeStatus(movie, 'STOPPED')} title="Ngừng chiếu"><span className="material-symbols-outlined">block</span></button>}
                      {movie.status !== 'NOW_SHOWING' && <button type="button" style={S.iconButton} onClick={() => changeStatus(movie, 'NOW_SHOWING')} title="Đang chiếu"><span className="material-symbols-outlined">visibility</span></button>}
                      <button type="button" style={S.iconButtonDanger} onClick={() => remove(movie)} title="Xóa"><span className="material-symbols-outlined">delete</span></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {modal && (
        <div style={S.overlay} onClick={() => setModal(null)}>
          <form style={S.modal} onSubmit={save} onClick={(event) => event.stopPropagation()}>
            <h2 style={S.modalTitle}>{editingId ? 'Cập nhật phim' : 'Thêm phim mới'}</h2>
            <div style={S.formGrid}>
              <Field label="Tên phim *"><input style={S.input} value={form.title} onChange={update('title')} /></Field>
              <Field label="Thể loại"><input style={S.input} value={form.genre} onChange={update('genre')} /></Field>
              <Field label="Thời lượng phút *"><input type="number" min="1" style={S.input} value={form.durationMinutes} onChange={update('durationMinutes')} /></Field>
              <Field label="Độ tuổi"><input style={S.input} value={form.ageRating} onChange={update('ageRating')} /></Field>
              <Field label="Ngày khởi chiếu"><input type="date" style={S.input} value={form.releaseDate} onChange={update('releaseDate')} /></Field>
              <Field label="Trạng thái *">
                <select style={S.input} value={form.status} onChange={update('status')}>
                  {Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </Field>
              <Field label="Độ hot / popularityScore">
                <input type="number" min="1" max="100" style={S.input} value={form.popularityScore} onChange={update('popularityScore')} />
              </Field>
              <Field label="Poster URL"><input style={S.input} value={form.posterUrl} onChange={update('posterUrl')} /></Field>
              <Field label="Trailer YouTube URL"><input style={S.input} value={form.trailerUrl} onChange={update('trailerUrl')} /></Field>
              <label style={{ ...S.field, gridColumn: '1 / -1' }}>
                <span style={S.label}>Mô tả</span>
                <textarea style={{ ...S.input, minHeight: 96, resize: 'vertical' }} value={form.description} onChange={update('description')} />
              </label>
            </div>
            <div style={S.previewRow}>
              <Poster src={form.posterUrl} title={form.title} />
              <button type="button" style={S.secondaryButton} onClick={() => previewTrailer(form)}>Xem trailer</button>
            </div>
            <div style={S.modalActions}>
              <button type="button" style={S.secondaryButton} onClick={() => setModal(null)}>Hủy</button>
              <button type="submit" style={S.primaryButton} disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu phim'}</button>
            </div>
          </form>
        </div>
      )}

      {trailer && (
        <div style={S.overlay} onClick={() => setTrailer(null)}>
          <div style={S.trailerModal} onClick={(event) => event.stopPropagation()}>
            <div style={S.trailerHeader}>
              <h2 style={S.modalTitle}>{trailer.title}</h2>
              <button type="button" style={S.iconButton} onClick={() => setTrailer(null)}><span className="material-symbols-outlined">close</span></button>
            </div>
            <div style={S.trailerFrame}>
              <iframe
                src={trailer.embedUrl}
                title="Trailer"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ width: '100%', height: '100%', border: 0 }}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Field({ label, children }) {
  return <label style={S.field}><span style={S.label}>{label}</span>{children}</label>
}

function Poster({ src, title }) {
  const [failed, setFailed] = useState(false)
  return <img src={!src || failed ? FALLBACK_POSTER : src} alt={title || 'Poster'} onError={() => setFailed(true)} style={S.poster} />
}

function extractYouTubeId(value) {
  if (!value) return ''
  const trimmed = value.trim()
  if (/^[A-Za-z0-9_-]{11}$/.test(trimmed)) return trimmed
  const patterns = [
    /youtube\.com\/watch\?[^#]*v=([A-Za-z0-9_-]{11})/,
    /youtu\.be\/([A-Za-z0-9_-]{11})/,
    /youtube\.com\/embed\/([A-Za-z0-9_-]{11})/,
    /youtube\.com\/shorts\/([A-Za-z0-9_-]{11})/,
  ]
  return patterns.map((pattern) => trimmed.match(pattern)?.[1]).find(Boolean) || ''
}

function errorText(err, fallback) {
  return err.response?.data?.message || err.response?.data || err.message || fallback
}

function statusBadge(status) {
  const colors = {
    NOW_SHOWING: ['rgba(34, 197, 94, 0.18)', '#bbf7d0'],
    COMING_SOON: ['rgba(59, 130, 246, 0.18)', '#bfdbfe'],
    STOPPED: ['rgba(148, 163, 184, 0.16)', 'var(--color-text-muted)'],
  }[status] || ['var(--surface-container-high)', 'var(--color-text-muted)']
  return { background: colors[0], color: colors[1], borderRadius: 999, padding: '4px 10px', fontSize: 12, fontWeight: 800, whiteSpace: 'nowrap' }
}

const S = {
  page: { padding: '24px', maxWidth: 1440, margin: '0 auto', width: '100%', minHeight: '100vh' },
  header: { display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', marginBottom: 20 },
  title: { margin: 0, color: 'var(--color-text)', fontSize: 28, fontWeight: 850 },
  subtitle: { margin: '6px 0 0', color: 'var(--color-text-muted)', fontSize: 14 },
  card: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: 'var(--shadow-card)' },
  filterGrid: { display: 'grid', gridTemplateColumns: 'minmax(260px, 1fr) 220px 220px', gap: 12 },
  input: { width: '100%', border: '1px solid var(--color-border)', borderRadius: 8, background: 'var(--surface-container-low)', color: 'var(--color-text)', padding: '10px 12px', fontSize: 14, boxSizing: 'border-box' },
  primaryButton: { border: 'none', borderRadius: 8, padding: '10px 14px', background: 'var(--color-primary)', color: 'var(--on-secondary-container)', fontWeight: 850, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 8 },
  secondaryButton: { border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 14px', background: 'var(--surface-container-high)', color: 'var(--color-text)', fontWeight: 800, cursor: 'pointer' },
  table: { width: '100%', borderCollapse: 'collapse', minWidth: 1080 },
  th: { padding: '12px 14px', textAlign: 'left', fontSize: 12, color: 'var(--color-text-muted)', background: 'var(--surface-container-high)', textTransform: 'uppercase', letterSpacing: 0 },
  td: { padding: '13px 14px', borderTop: '1px solid var(--color-border)', color: 'var(--color-text)', fontSize: 14, verticalAlign: 'middle' },
  poster: { width: 56, height: 82, objectFit: 'cover', borderRadius: 6, border: '1px solid var(--color-border)', background: 'var(--surface-container-high)' },
  iconButton: { border: '1px solid var(--color-border)', background: 'var(--surface-container-high)', color: 'var(--color-text)', borderRadius: 8, width: 36, height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' },
  iconButtonDanger: { border: '1px solid rgba(248, 113, 113, 0.25)', background: 'rgba(185, 28, 28, 0.16)', color: '#fecaca', borderRadius: 8, width: 36, height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' },
  success: { marginBottom: 14, background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', borderRadius: 8, padding: 12 },
  error: { marginBottom: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', borderRadius: 8, padding: 12 },
  overlay: { position: 'fixed', inset: 0, background: 'rgba(3, 7, 18, 0.72)', backdropFilter: 'blur(4px)', zIndex: 500, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 18 },
  modal: { width: 760, maxWidth: '96vw', maxHeight: '92vh', overflowY: 'auto', background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 24, boxShadow: '0 25px 70px rgba(0,0,0,0.45)' },
  modalTitle: { margin: 0, color: 'var(--color-text)', fontSize: 20, fontWeight: 850 },
  formGrid: { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12, marginTop: 18 },
  field: { display: 'block', minWidth: 0 },
  label: { display: 'block', color: 'var(--color-text-muted)', fontSize: 13, fontWeight: 800, marginBottom: 6 },
  previewRow: { marginTop: 16, display: 'flex', alignItems: 'center', gap: 12 },
  modalActions: { marginTop: 22, display: 'flex', justifyContent: 'flex-end', gap: 10 },
  trailerModal: { width: 900, maxWidth: '96vw', background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: '0 25px 70px rgba(0,0,0,0.45)' },
  trailerHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 14 },
  trailerFrame: { aspectRatio: '16 / 9', width: '100%', borderRadius: 8, overflow: 'hidden', background: '#030712' },
}
