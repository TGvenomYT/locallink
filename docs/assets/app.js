/* LocalLink landing — interactions: theme, nav, reveal, mesh canvas */
(function () {
  "use strict";

  /* ---------- Theme toggle (persisted) ---------- */
  var root = document.documentElement;
  var saved = null;
  try { saved = localStorage.getItem("ll-theme"); } catch (e) {}
  if (saved === "light" || saved === "dark") {
    root.setAttribute("data-theme", saved);
  }
  var toggle = document.getElementById("theme-toggle");
  if (toggle) {
    toggle.addEventListener("click", function () {
      var next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
      root.setAttribute("data-theme", next);
      try { localStorage.setItem("ll-theme", next); } catch (e) {}
    });
  }

  /* ---------- Mobile nav hamburger ---------- */
  var mobileBtn = document.getElementById("mobile-menu");
  var mobileNav = document.getElementById("mobile-nav");
  if (mobileBtn && mobileNav) {
    var hamOpen = mobileBtn.querySelector(".ham-open");
    var hamClose = mobileBtn.querySelector(".ham-close");
    function toggleMenu() {
      var open = mobileNav.classList.toggle("open");
      mobileBtn.setAttribute("aria-expanded", open ? "true" : "false");
      mobileNav.setAttribute("aria-hidden", open ? "false" : "true");
      if (hamOpen) hamOpen.style.display = open ? "none" : "block";
      if (hamClose) hamClose.style.display = open ? "block" : "none";
    }
    mobileBtn.addEventListener("click", toggleMenu);
    // Close menu when a link is tapped
    mobileNav.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        mobileNav.classList.remove("open");
        mobileBtn.setAttribute("aria-expanded", "false");
        mobileNav.setAttribute("aria-hidden", "true");
        if (hamOpen) hamOpen.style.display = "block";
        if (hamClose) hamClose.style.display = "none";
      });
    });
  }

  /* ---------- Sticky nav shadow ---------- */
  var nav = document.getElementById("nav");
  function onScroll() {
    if (!nav) return;
    nav.classList.toggle("scrolled", window.scrollY > 12);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ---------- Reveal on scroll ---------- */
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var targets = document.querySelectorAll(
    ".section-head, .card, .step, .stat, .problem-lead, .download-card, .hero-inner"
  );
  targets.forEach(function (el) { el.classList.add("reveal"); });
  if (reduce || !("IntersectionObserver" in window)) {
    targets.forEach(function (el) { el.classList.add("in"); });
  } else {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("in");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    targets.forEach(function (el) { io.observe(el); });
  }

  /* ---------- Animated mesh background ---------- */
  var canvas = document.getElementById("mesh-bg");
  if (!canvas || reduce) return;
  var ctx = canvas.getContext("2d");
  var w, h, dpr, nodes, raf;
  var LINK_DIST = 150;

  function accentColor() {
    // Read the live accent so the canvas follows the theme.
    return getComputedStyle(root).getPropertyValue("--accent").trim() || "#32d74b";
  }
  var accent = accentColor();

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    w = window.innerWidth;
    h = window.innerHeight;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    var count = Math.min(70, Math.round((w * h) / 26000));
    // Fewer particles on mobile for smoother 60fps
    if (w < 600) count = Math.min(count, 28);
    nodes = [];
    for (var i = 0; i < count; i++) {
      nodes.push({
        x: Math.random() * w,
        y: Math.random() * h,
        vx: (Math.random() - 0.5) * 0.28,
        vy: (Math.random() - 0.5) * 0.28
      });
    }
    accent = accentColor();
  }

  function hexToRgb(hex) {
    var m = hex.replace("#", "");
    if (m.length === 3) m = m[0] + m[0] + m[1] + m[1] + m[2] + m[2];
    var n = parseInt(m, 16);
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
  }

  function frame() {
    ctx.clearRect(0, 0, w, h);
    var rgb = hexToRgb(accent);
    var base = "rgba(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + ",";

    for (var i = 0; i < nodes.length; i++) {
      var a = nodes[i];
      a.x += a.vx; a.y += a.vy;
      if (a.x < 0 || a.x > w) a.vx *= -1;
      if (a.y < 0 || a.y > h) a.vy *= -1;

      for (var j = i + 1; j < nodes.length; j++) {
        var b = nodes[j];
        var dx = a.x - b.x, dy = a.y - b.y;
        var dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < LINK_DIST) {
          ctx.strokeStyle = base + (0.16 * (1 - dist / LINK_DIST)).toFixed(3) + ")";
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(a.x, a.y);
          ctx.lineTo(b.x, b.y);
          ctx.stroke();
        }
      }
      ctx.fillStyle = base + "0.6)";
      ctx.beginPath();
      ctx.arc(a.x, a.y, 1.6, 0, Math.PI * 2);
      ctx.fill();
    }
    raf = requestAnimationFrame(frame);
  }

  window.addEventListener("resize", resize);
  resize();
  frame();

  // Keep colors in sync when the theme flips.
  if (toggle) {
    toggle.addEventListener("click", function () {
      // Defer so the attribute + CSS var have updated.
      setTimeout(function () { accent = accentColor(); }, 50);
    });
  }
})();
