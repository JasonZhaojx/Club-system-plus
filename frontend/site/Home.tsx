import { Link } from 'react-router-dom'
import { socials } from './config'

export default function Home() {
  return <>
    <section className="home-hero" aria-labelledby="home-heading"><div className="container"><div className="hero-panel"><p className="eyebrow">UNSWCSA</p><h1 id="home-heading">新南学联</h1><p className="hero-caption">品牌标语待确认</p><Link className="button" to="/about">了解我们 <span aria-hidden="true">↗</span></Link></div></div></section>
    <div className="container">
      <p className="preview-note">视觉开发预览 · 正式文案、照片与外部链接尚待补充</p>
      <div className="intro-grid section-space">
        <section aria-labelledby="intro-heading"><h2 id="intro-heading">学联简介</h2><p className="muted">正式介绍待补充。</p><p className="muted">这里将以一至两段短文介绍学联。</p></section>
        <section aria-labelledby="honors-heading"><h2 id="honors-heading">荣誉与影响力</h2><ul className="honors"><li>待补充经核实的荣誉</li><li>待补充代表性成果</li></ul></section>
      </div>
      <section className="section-space brand-section" aria-labelledby="brand-heading">
        <div className="section-heading"><h2 id="brand-heading">品牌活动</h2><Link className="text-link" to="/brand-events">浏览品牌活动 <span aria-hidden="true">↗</span></Link></div>
        <div className="brand-grid">{[1, 2].map(number => <article className="brand-card" key={number}><div className="image-placeholder"><span className="placeholder-frame" aria-hidden="true">＋</span><span>活动照片待提供</span></div><div className="card-copy"><p className="eyebrow">内容预留</p><h3>品牌活动名称待补充</h3><p className="muted">代表性活动的简短介绍待补充。</p><span className="pending-label">详情待补充</span></div></article>)}</div>
      </section>
    </div>
    <section className="join-strip" aria-labelledby="join-heading"><div className="container join-inner"><div><p className="eyebrow">JOIN UNSWCSA</p><h2 id="join-heading">加入我们</h2><p className="muted">加入介绍与招新信息待补充。</p></div><Link className="button button-red" to="/join">了解招新 <span aria-hidden="true">↗</span></Link></div></section>
    <section className="container social-section" aria-labelledby="social-heading"><div className="section-heading"><h2 id="social-heading">关注我们</h2><p className="muted">官方入口待确认</p></div><ul className="social-grid">{socials.map(item => <li key={item.name}><div><span>{item.name}</span><small>链接待补充</small></div></li>)}</ul></section>
  </>
}
