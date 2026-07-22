import React from 'react'
import AppNavbar from '../../components/AppNavbar'
import Hero from '../../components/Hero'
import NowShowing from '../../components/NowShowing'
import Promo from '../../components/Promo'
import Footer from '../../components/Footer'

export default function Home() {
  return (
    <>
      <AppNavbar />
      <Hero />
      <NowShowing />
      <Promo />
      <Footer />
    </>
  )
}
